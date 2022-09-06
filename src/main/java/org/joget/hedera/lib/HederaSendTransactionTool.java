package org.joget.hedera.lib;

import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ScheduleCreateTransaction;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.ExplorerUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;

public class HederaSendTransactionTool extends DefaultApplicationPlugin {

    @Override
    public String getName() {
        return "Hedera Send Transaction Tool";
    }

    @Override
    public String getVersion() {
        return PluginUtil.getProjectVersion(this.getClass());
    }

    @Override
    public String getDescription() {
        return "Send funds from one account to another on the Hedera DLT, with option to perform scheduled transactions.";
    }

    @Override
    public Object execute(Map props) {
        final String operatorId = getPropertyString("operatorId");
        final String operatorKey = getPropertyString("operatorKey");
        final String networkType = getPropertyString("networkType");
        
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        try {
            TransactionRecord transactionRecord = null;
            
            final String senderAccountId = WorkflowUtil.processVariable(getPropertyString("senderAccountId"), "", wfAssignment);
            final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
            final String receiverAccountId = WorkflowUtil.processVariable(getPropertyString("receiverAccountId"), "", wfAssignment);
            final String amount = WorkflowUtil.processVariable(getPropertyString("amount"), "", wfAssignment);
            final String enableScheduledTxFlag = getPropertyString("enableScheduledTransaction");
            
            final Client client = BackendUtil.getHederaClient(operatorId, operatorKey, networkType);
            
            if (client != null) {
                AccountId senderAccount = AccountId.fromString(senderAccountId);
                AccountId receiverAccount = AccountId.fromString(receiverAccountId);
                
                //1 hbar = 100,000,000 tinybars
                Hbar amountHbar = Hbar.fromString(amount);
                
                TransferTransaction transferTransaction = new TransferTransaction()
                    .addHbarTransfer(senderAccount, amountHbar.negated())
                    .addHbarTransfer(receiverAccount, amountHbar);
                      //Can set a transaction memo of string up to max length of 100
//                    .setTransactionMemo("joget transfer test")

                TransactionResponse transactionResponse;

                if ("true".equals(enableScheduledTxFlag)) {
                    ScheduleCreateTransaction scheduledTransaction = new ScheduleCreateTransaction()
                        .setScheduledTransaction(transferTransaction)
                        .setAdminKey(client.getOperatorPublicKey())
                        .setPayerAccountId(senderAccount)
                        .freezeWith(client);
                    transactionResponse = scheduledTransaction.execute(client);
                } else {
                    if (accountMnemonic == null || accountMnemonic.isEmpty()) {
                        LogUtil.warn(getClass().getName(), "Plugin execution stopped. Invalid mnemonic encountered.");
                        return null;
                    }
                    final PrivateKey senderPrivateKey = PluginUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));
                    transferTransaction.freezeWith(client);
                    transferTransaction.sign(senderPrivateKey);
                    transactionResponse = transferTransaction.execute(client);
                }
                
                transactionRecord = transactionResponse.getRecord(client);
                
                storeToWorkflowVariable(wfAssignment, props, networkType, transactionRecord);
            }
            
            return transactionRecord;
        } catch (Exception ex) {
            LogUtil.error(getClass().getName(), ex, "Error executing plugin...");
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
    
    protected void storeToWorkflowVariable(WorkflowAssignment wfAssignment, Map properties, String networkType, TransactionRecord transactionRecord) {
        
        String wfScheduleId = getPropertyString("wfScheduleId");
        String wfTransactionValidated = getPropertyString("wfTransactionValidated");
        String wfConsensusTimestamp = getPropertyString("wfConsensusTimestamp");
        String wfTransactionId = getPropertyString("wfTransactionId");
        String wfTransactionExplorerUrl = getPropertyString("wfTransactionExplorerUrl");

        ApplicationContext ac = AppUtil.getApplicationContext();
        WorkflowManager workflowManager = (WorkflowManager) ac.getBean("workflowManager");
        
        storeValuetoActivityVar(
                workflowManager, 
                wfAssignment.getActivityId(), 
                wfScheduleId, 
                transactionRecord.receipt.scheduleId != null ? transactionRecord.receipt.scheduleId.toString() : ""
        );
        
        storeValuetoActivityVar(
                workflowManager, 
                wfAssignment.getActivityId(), 
                wfTransactionValidated, 
                transactionRecord.receipt.status.toString()
        );
        
        storeValuetoActivityVar(
                workflowManager,
                wfAssignment.getActivityId(), 
                wfConsensusTimestamp, 
                TransactionUtil.convertInstantToZonedDateTimeString(transactionRecord.consensusTimestamp)
        );
        
        storeValuetoActivityVar(
                workflowManager, 
                wfAssignment.getActivityId(), 
                wfTransactionId, 
                transactionRecord.transactionId.toString()
        );
        
        String transactionExplorerUrl = ExplorerUtil.getTransactionExplorerUrl(networkType, transactionRecord.transactionId.toString());
        storeValuetoActivityVar(
                workflowManager, 
                wfAssignment.getActivityId(), 
                wfTransactionExplorerUrl, 
                transactionExplorerUrl != null ? transactionExplorerUrl : "Not available"
        );
    }
    
    private void storeValuetoActivityVar(WorkflowManager workflowManager, String activityId, String variable, String value) {
        if (!variable.isEmpty() && value != null) {
            workflowManager.activityVariable(activityId, variable, value);
        }
    }
    
    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/HederaSendTransactionTool.json", null, true, "messages/HederaMessages");
    }
}
