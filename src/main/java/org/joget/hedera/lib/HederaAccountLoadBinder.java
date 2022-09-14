package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HbarUnit;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadElementBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.hedera.model.HederaFormBinderAbstract;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaAccountLoadBinder extends HederaFormBinderAbstract implements FormLoadElementBinder {

    @Override
    public String getName() {
        return "Hedera Account Load Binder";
    }

    @Override
    public String getDescription() {
        return "Load account data from the Hedera DLT into a form.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaAccountLoadBinder.json", new String[]{backendConfigs}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid() {
        final String accountId = WorkflowUtil.processVariable(getPropertyString("accountId"), "", null);

        //Prevent error thrown from empty value and invalid hash variable
        return !accountId.isEmpty() && !accountId.startsWith("#");
    }
    
    @Override
    public FormRowSet loadData(Client client, Element element, String primaryKey, FormData formData) 
            throws TimeoutException, PrecheckStatusException {
        
        final String accountId = WorkflowUtil.processVariable(getPropertyString("accountId"), "", null);

        AccountInfo accountInfo = new AccountInfoQuery()
            .setAccountId(AccountId.fromString(accountId))
            .execute(client);

        //Get form fields from plugin properties
        String balanceField = getPropertyString("balanceField");
        String accountMemoField = getPropertyString("accountMemoField");
        String accountIsDeletedField = getPropertyString("accountIsDeletedField");
        String receiverSignatureRequiredField = getPropertyString("receiverSignatureRequiredField");
        String ownedNftsField = getPropertyString("ownedNftsField");
        String sendRecordThresholdField = getPropertyString("sendRecordThresholdField");
        String receiveRecordThresholdField = getPropertyString("receiveRecordThresholdField");

        FormRow row = new FormRow();
        row = addRow(row, balanceField, accountInfo.balance.toString(HbarUnit.HBAR));
        row = addRow(row, accountMemoField, accountInfo.accountMemo);
        row = addRow(row, accountIsDeletedField, String.valueOf(accountInfo.isDeleted));
        row = addRow(row, receiverSignatureRequiredField, String.valueOf(accountInfo.isReceiverSignatureRequired));
        row = addRow(row, ownedNftsField, String.valueOf(accountInfo.ownedNfts));
        row = addRow(row, sendRecordThresholdField, accountInfo.sendRecordThreshold.toString(HbarUnit.HBAR));
        row = addRow(row, receiveRecordThresholdField, accountInfo.receiveRecordThreshold.toString(HbarUnit.HBAR));

        FormRowSet rows = new FormRowSet();
        rows.add(row);
        
        return rows;
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        
        return row;
    }
}
