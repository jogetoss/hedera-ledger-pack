package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleInfo;
import com.hedera.hashgraph.sdk.ScheduleInfoQuery;
import com.hedera.hashgraph.sdk.ScheduleSignTransaction;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

public class HederaSignScheduledTransactionTool extends DefaultApplicationPlugin {

    @Override
    public String getName() {
        return "Hedera Sign Scheduled Transaction Tool";
    }

    @Override
    public String getVersion() {
        return PluginUtil.getProjectVersion(this.getClass());
    }

    @Override
    public String getDescription() {
        return "Sign a scheduled transaction that is queued in the Hedera network.";
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
            
            final Client client = BackendUtil.getHederaClient(operatorId, operatorKey, networkType);
            
            if (client != null) {
//                final String signerAccountId = WorkflowUtil.processVariable(getPropertyString("signerAccountId"), "", wfAssignment);
                final String signerMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("signerMnemonic"), "", wfAssignment));
                final PrivateKey signerPrivateKey = PluginUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(signerMnemonic));
                final PublicKey signerPublicKey = PluginUtil.derivePublicKeyFromMnemonic(Mnemonic.fromString(signerMnemonic));
                
                final String scheduleId = WorkflowUtil.processVariable(getPropertyString("scheduleId"), "", null);
                ScheduleId scheduleIdObj = ScheduleId.fromString(scheduleId);
                
                ScheduleInfo info = new ScheduleInfoQuery()
                    .setScheduleId(scheduleIdObj)
                    .execute(client);
                if (info.signatories.contains(signerPublicKey)) {
                    LogUtil.warn(getClass().getName(), "Signer has already signed this transaction! Skipping plugin execution...");
                    return null;
                }
                
                transactionRecord = new ScheduleSignTransaction()
                    .setScheduleId(scheduleIdObj)
                    .freezeWith(client)
                    .sign(signerPrivateKey)
                    .execute(client)
                    .getRecord(client);
                
                storeToWorkflowVariable(wfAssignment, transactionRecord);
            }
            
            return transactionRecord;
        } catch (Exception ex) {
            LogUtil.error(getClass().getName(), ex, "Error executing plugin...");
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/HederaSignScheduledTransactionTool.json", null, true, "messages/HederaMessages");
    }
}
