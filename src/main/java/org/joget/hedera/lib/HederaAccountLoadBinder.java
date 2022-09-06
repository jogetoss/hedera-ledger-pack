package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HbarUnit;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormLoadElementBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaAccountLoadBinder extends FormBinder implements FormLoadBinder, FormLoadElementBinder {

    @Override
    public String getName() {
        return "Hedera Account Load Binder";
    }

    @Override
    public String getVersion() {
        return PluginUtil.getProjectVersion(this.getClass());
    }

    @Override
    public String getDescription() {
        return "Load account data from the Hedera DLT into a form.";
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final String accountId = WorkflowUtil.processVariable(getPropertyString("accountId"), "", null);

        //Prevent error thrown from empty value and invalid hash variable
        if (accountId.isEmpty() || accountId.startsWith("#")) {
            return null;
        }
        
        final String operatorId = getPropertyString("operatorId");
        final String operatorKey = getPropertyString("operatorKey");
        final String networkType = getPropertyString("networkType");
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        FormRowSet rows = new FormRowSet();
        
        try {
            final Client client = BackendUtil.getHederaClient(operatorId, operatorKey, networkType);
            
            if (client != null) {
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
                
                rows.add(row);
            }
            
            return rows;
        } catch (Exception ex) {
            LogUtil.error(getClass().getName(), ex, "Error executing plugin...");
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        return row;
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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/HederaAccountLoadBinder.json", null, true, "messages/HederaMessages");
    }
}
