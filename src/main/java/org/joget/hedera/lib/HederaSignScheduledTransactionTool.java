package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.BadMnemonicException;
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
import org.joget.hedera.model.HederaProcessTool;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;

/**
* @deprecated
* Pending rework in tandem with hashconnect.
*/
@Deprecated
public class HederaSignScheduledTransactionTool extends HederaProcessTool {

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
    protected Object runTool(Map props) 
            throws TimeoutException, RuntimeException {
        
        try {    
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

            storeAdditionalDataToWorkflowVariable(transactionRecord);

            return transactionRecord;
        } catch (PrecheckStatusException | BadMnemonicException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    protected void storeAdditionalDataToWorkflowVariable(TransactionRecord transactionRecord) {
        String wfResponseStatus = getPropertyString("wfResponseStatus");
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfResponseStatus, 
                transactionRecord.receipt.status.toString()
        );
    }
}
