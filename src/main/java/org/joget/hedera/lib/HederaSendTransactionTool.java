package org.joget.hedera.lib;

import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.ScheduleCreateTransaction;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.util.concurrent.TimeoutException;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.hedera.model.HederaProcessToolAbstract;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.ExplorerUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;

public class HederaSendTransactionTool extends HederaProcessToolAbstract {
    
    @Override
    public String getName() {
        return "Hedera Send Transaction Tool";
    }

    @Override
    public String getDescription() {
        return "Send funds from one account to another on the Hedera DLT, with option to perform scheduled transactions.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaSendTransactionTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid(Map props) {
        final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
        
        try {
            Mnemonic.fromString(accountMnemonic);
        } catch (BadMnemonicException ex) {
            LogUtil.warn(getClassName(), "Send transaction aborted. Sender account encountered bad/invalid mnemonic phrase.");
            return false;
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Send transaction aborted. Unexpected error when attempting to validate mnemonic phrase.");
            return false;
        }
        
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, primaryKey);
        
        if (rowSet == null || rowSet.isEmpty()) {
            LogUtil.warn(getClassName(), "Send transaction aborted. No record found with record ID '" + primaryKey + "' from this form '" + formDefId + "'");
            return false;
        }
        
        return true;
    }
    
    @Override
    protected Object runTool(Map props, Client client) 
            throws TimeoutException, PrecheckStatusException, BadMnemonicException, ReceiptStatusException {
        
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, primaryKey);
        
        FormRow row = rowSet.get(0);
        
        final String senderAccountId = row.getProperty(getPropertyString("senderAccountId"));
        final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
        final String receiverAccountId = row.getProperty(getPropertyString("receiverAccountId"));
        final String amount = row.getProperty(getPropertyString("amount"));
        final boolean enableScheduledTx = "true".equals(getPropertyString("enableScheduledTransaction"));

        final AccountId senderAccount = AccountId.fromString(senderAccountId);
        final AccountId receiverAccount = AccountId.fromString(receiverAccountId);
        final PrivateKey senderPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));
        
        //1 hbar = 100,000,000 tinybars
        Hbar amountHbar = Hbar.fromString(amount);

        TransferTransaction transferTransaction = new TransferTransaction()
                .addHbarTransfer(senderAccount, amountHbar.negated())
                .addHbarTransfer(receiverAccount, amountHbar);
                
              //Can set a transaction memo of string up to max length of 100
//                    .setTransactionMemo("joget transfer test")

        TransactionResponse transactionResponse;

        if (enableScheduledTx) {
            ScheduleCreateTransaction scheduledTransaction = new ScheduleCreateTransaction()
                .setScheduledTransaction(transferTransaction)
                .setAdminKey(client.getOperatorPublicKey())
                .setPayerAccountId(senderAccount)
                .freezeWith(client)
                .sign(senderPrivateKey);
            transactionResponse = scheduledTransaction.execute(client);
        } else {
            transferTransaction.freezeWith(client).sign(senderPrivateKey);
            transactionResponse = transferTransaction.execute(client);
        }

        TransactionRecord transactionRecord = transactionResponse.getRecord(client);

        storeToWorkflowVariable(props, transactionRecord);

        return transactionRecord;
    }
    
    protected void storeToWorkflowVariable(Map properties, TransactionRecord transactionRecord) {
        
        final String networkType = BackendUtil.getNetworkType(properties);
        
        String wfScheduleId = getPropertyString("wfScheduleId");
        String wfTransactionValidated = getPropertyString("wfTransactionValidated");
        String wfConsensusTimestamp = getPropertyString("wfConsensusTimestamp");
        String wfTransactionId = getPropertyString("wfTransactionId");
        String wfTransactionExplorerUrl = getPropertyString("wfTransactionExplorerUrl");
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfScheduleId, 
                transactionRecord.receipt.scheduleId != null ? transactionRecord.receipt.scheduleId.toString() : ""
        );
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfTransactionValidated, 
                transactionRecord.receipt.status.toString()
        );
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfConsensusTimestamp, 
                TransactionUtil.convertInstantToZonedDateTimeString(transactionRecord.consensusTimestamp)
        );
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfTransactionId, 
                transactionRecord.transactionId.toString()
        );
        
        String transactionExplorerUrl = ExplorerUtil.getTransactionExplorerUrl(networkType, transactionRecord.transactionId.toString());
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfTransactionExplorerUrl, 
                transactionExplorerUrl != null ? transactionExplorerUrl : "Not available"
        );
    }
    
    private void storeValuetoActivityVar(String activityId, String variable, String value) {
        if (!variable.isEmpty() && value != null) {
            workflowManager.activityVariable(activityId, variable, value);
        }
    }
}
