package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaProcessTool;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaGenerateAccountTool extends HederaProcessTool {

    @Override
    public String getName() {
        return "Hedera Generate Account Tool";
    }

    @Override
    public String getDescription() {
        return "Generates a new account on the Hedera DLT.";
    }
    
    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaGenerateAccountTool.json", new String[]{backendConfigs}, true, PluginUtil.MESSAGE_PATH);
    }

    @Override
    protected Object runTool(Map props) 
            throws TimeoutException, RuntimeException {
        
        try {
            final boolean fundTestAccount = "true".equals(getPropertyString("fundTestAccount"));
            final String accountMemo = WorkflowUtil.processVariable(getPropertyString("accountMemo"), "", wfAssignment);
            final String formDefIdGetData = getPropertyString("formDefIdGetData");
            final String getMnemonicField = getPropertyString("getMnemonicField");
            final String accountIdsToSign = WorkflowUtil.processVariable(getPropertyString("accountIdsToSign"), "", wfAssignment);
            final boolean isTest = client.getLedgerId().isTestnet() || client.getLedgerId().isPreviewnet();
            final boolean isMultiSig = "true".equals(getPropertyString("enableMultiSig"));

            String encryptedMnemonic = "";
            String accountSigners = "";
            AccountCreateTransaction newAccountTransaction = new AccountCreateTransaction();

            if (isMultiSig) {
                KeyList keyList = new KeyList();

                for (String accountId : accountIdsToSign.split(PluginUtil.MULTI_VALUE_DELIMITER)) {
                    FormRowSet accountRowSet = getFormRecord(formDefIdGetData, accountId);
                    if (!accountRowSet.isEmpty()) {
                        FormRow accountData = accountRowSet.get(0);
                        Mnemonic signerMnemonic = Mnemonic.fromString(PluginUtil.decrypt(accountData.getProperty(getMnemonicField)));
                        PublicKey signerPublicKey = AccountUtil.derivePublicKeyFromMnemonic(signerMnemonic);

                        keyList.add(signerPublicKey);
                    }
                }

                accountSigners = accountIdsToSign;

                newAccountTransaction.setKey(keyList);
            } else {
                final Mnemonic mnemonic = Mnemonic.generate24();
                PublicKey publicKey = AccountUtil.derivePublicKeyFromMnemonic(mnemonic);
                //Account Mnemonic MUST be secured at all times.
                /* 
                    See HederaUtil encrypt & decrypt method to implement your preferred algo if you wish to do so
                */
                encryptedMnemonic = PluginUtil.encrypt(mnemonic.toString());

                newAccountTransaction.setKey(publicKey);
            }

            if (isTest && fundTestAccount) {
                fundTestAccount(newAccountTransaction);
            }

            if (!accountMemo.isBlank()) {
                newAccountTransaction.setAccountMemo(accountMemo);
            }

            // This will wait for the tx to complete and get a "receipt" as response. Fyi there is also async methods available if needed.
            TransactionReceipt receipt = newAccountTransaction
                    .execute(client)
                    .getReceipt(client);

            storeToForm(props, isTest, encryptedMnemonic, isMultiSig, accountSigners, receipt);
            storeAdditionalDataToWorkflowVariable(props, isTest, receipt);

            return receipt;
        } catch (PrecheckStatusException | BadMnemonicException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    private void fundTestAccount(AccountCreateTransaction newAccountTransaction) {
        // At time of writing, operator accounts will always be 10,000 hbars, topped up every 24 hours.
        // Can make the initial balance a configurable plugin property if you want to
        newAccountTransaction.setInitialBalance(Hbar.from(100));
    }
    
    protected void storeToForm(Map properties, boolean isTest, final String encryptedMnemonic, final boolean isMultiSig, final String accountSigners, final TransactionReceipt receipt) {
        String formDefId = getPropertyString("formDefId");
        
        if (formDefId != null && formDefId.trim().length() > 0) {
            String accountMnemonicField = getPropertyString("accountMnemonicField");
            String accountOwnerField = getPropertyString("accountOwnerField");
            String accountOwnerValue = WorkflowUtil.processVariable(getPropertyString("accountOwnerValue"), "", wfAssignment);
            String isTestAccountField = getPropertyString("isTestAccount");
            String isMultiSigAccountField = getPropertyString("isMultiSigAccount");
            String accountSignersField = getPropertyString("accountSigners");
            
            FormRow row = new FormRow();
            
            //Account ID set as Record ID
            row.setId(receipt.accountId.toString());
            row = addRow(row, accountMnemonicField, encryptedMnemonic);
            row = addRow(row, accountOwnerField, accountOwnerValue);
            row = addRow(row, isTestAccountField, String.valueOf(isTest));
            row = addRow(row, isMultiSigAccountField, String.valueOf(isMultiSig));
            row = addRow(row, accountSignersField, isMultiSig ? accountSigners : "");

            FormRowSet storedData = storeFormRow(formDefId, row);
            if (storedData == null) {
                LogUtil.warn(getClassName(), "Unable to store account data to form. Encountered invalid form ID of '" + formDefId + "'.");
            }
        }
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        
        return row;
    }
    
    protected void storeAdditionalDataToWorkflowVariable(Map properties, boolean isTest, final TransactionReceipt receipt) {
        String wfResponseStatus = getPropertyString("wfResponseStatus");
        String wfIsTestAccount = getPropertyString("wfIsTestAccount");
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfResponseStatus, 
                receipt.status.toString()
        );
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfIsTestAccount, 
                String.valueOf(isTest)
        );
    }
}
