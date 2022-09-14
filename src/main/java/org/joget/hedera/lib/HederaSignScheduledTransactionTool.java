package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleInfo;
import com.hedera.hashgraph.sdk.ScheduleInfoQuery;
import com.hedera.hashgraph.sdk.ScheduleSignTransaction;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaProcessToolAbstract;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

public class HederaSignScheduledTransactionTool extends HederaProcessToolAbstract {

    @Override
    public String getName() {
        return "Hedera Sign Scheduled Transaction Tool";
    }

    @Override
    public String getDescription() {
        return "Sign a scheduled transaction that is queued in the Hedera network.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaSignScheduledTransactionTool.json", new String[]{backendConfigs}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public Object runTool(Map props, Client client, WorkflowAssignment wfAssignment) 
            throws TimeoutException, PrecheckStatusException, BadMnemonicException, ReceiptStatusException {
            
//        final String signerAccountId = WorkflowUtil.processVariable(getPropertyString("signerAccountId"), "", wfAssignment);
        final String signerMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("signerMnemonic"), "", wfAssignment));
        final PrivateKey signerPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(signerMnemonic));
        final PublicKey signerPublicKey = AccountUtil.derivePublicKeyFromMnemonic(Mnemonic.fromString(signerMnemonic));

        final String scheduleId = WorkflowUtil.processVariable(getPropertyString("scheduleId"), "", null);
        ScheduleId scheduleIdObj = ScheduleId.fromString(scheduleId);

        ScheduleInfo info = new ScheduleInfoQuery()
            .setScheduleId(scheduleIdObj)
            .execute(client);
        if (info.signatories.contains(signerPublicKey)) {
            LogUtil.warn(getClassName(), "Signer has already signed this transaction! Skipping plugin execution...");
            return null;
        }

        TransactionRecord transactionRecord = new ScheduleSignTransaction()
            .setScheduleId(scheduleIdObj)
            .freezeWith(client)
            .sign(signerPrivateKey)
            .execute(client)
            .getRecord(client);

        storeToWorkflowVariable(wfAssignment, transactionRecord);

        return transactionRecord;
    }
    
    protected void storeToWorkflowVariable(WorkflowAssignment wfAssignment, TransactionRecord transactionRecord) {
        String wfResponseStatus = getPropertyString("wfResponseStatus");
        
        ApplicationContext ac = AppUtil.getApplicationContext();
        WorkflowManager workflowManager = (WorkflowManager) ac.getBean("workflowManager");
        
        storeValuetoActivityVar(workflowManager, wfAssignment.getActivityId(), wfResponseStatus, transactionRecord.receipt.status.toString());
    }
    
    private void storeValuetoActivityVar(WorkflowManager workflowManager, String activityId, String variable, String value) {
        if (!variable.isEmpty() && value != null) {
            workflowManager.activityVariable(activityId, variable, value);
        }
    }
}
