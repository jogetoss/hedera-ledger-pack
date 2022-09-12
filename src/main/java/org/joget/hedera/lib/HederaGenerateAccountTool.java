package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.hedera.model.HederaProcessToolAbstract;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

public class HederaGenerateAccountTool extends HederaProcessToolAbstract {

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
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaGenerateAccountTool.json", null, true, PluginUtil.MESSAGE_PATH);
    }

    @Override
    public Object runTool(Map props, Client client, WorkflowAssignment wfAssignment) 
            throws TimeoutException, PrecheckStatusException, BadMnemonicException, ReceiptStatusException {
        
        final boolean fundTestAccount = "true".equals(getPropertyString("fundTestAccount"));
        final String formDefIdGetData = getPropertyString("formDefIdGetData");
        final String getMnemonicField = getPropertyString("getMnemonicField");
        final String accountIdsToSign = WorkflowUtil.processVariable(getPropertyString("accountIdsToSign"), "", wfAssignment);
        final boolean isTest = BackendUtil.isTestnet(props);
        final boolean isMultiSig = "true".equals(getPropertyString("enableMultiSig"));
            
        String encryptedMnemonic = "";
        String accountSigners = "";
        AccountCreateTransaction newAccountTransaction = new AccountCreateTransaction();

        if (isMultiSig) {
            ApplicationContext ac = AppUtil.getApplicationContext();
            AppService appService = (AppService) ac.getBean("appService");
            AppDefinition appDef = (AppDefinition) props.get("appDef");

            KeyList keyList = new KeyList();

            for (String accountId : accountIdsToSign.split(PluginUtil.MULTI_VALUE_DELIMITER)) {
                FormRowSet accountRowSet = appService.loadFormData(appDef.getId(), appDef.getVersion().toString(), formDefIdGetData, accountId);
                if (!accountRowSet.isEmpty()) {
                    FormRow accountData = accountRowSet.get(0);
                    Mnemonic signerMnemonic = Mnemonic.fromString(PluginUtil.decrypt(accountData.getProperty(getMnemonicField)));
                    PublicKey signerPublicKey = PluginUtil.derivePublicKeyFromMnemonic(signerMnemonic);

                    keyList.add(signerPublicKey);
                }
            }

            accountSigners = accountIdsToSign;

            newAccountTransaction.setKey(keyList);
        } else {
            final Mnemonic mnemonic = Mnemonic.generate24();
            PublicKey publicKey = PluginUtil.derivePublicKeyFromMnemonic(mnemonic);
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

        // This will wait for the tx to complete and get a "receipt" as response. Fyi there is also async methods available if needed.
        TransactionReceipt receipt = newAccountTransaction.execute(client).getReceipt(client);

        AccountId newAccountId = receipt.accountId;

        AccountInfo newAccountInfo = new AccountInfoQuery().setAccountId(newAccountId).execute(client);

        storeToForm(wfAssignment, props, isTest, encryptedMnemonic, isMultiSig, accountSigners, newAccountInfo);
        storeToWorkflowVariable(wfAssignment, props, isTest, receipt);
            
        return newAccountId;
    }
    
    private void fundTestAccount(AccountCreateTransaction newAccountTransaction) {
        // Can make the initial balance a configurable plugin property if you want to
        newAccountTransaction.setInitialBalance(Hbar.from(50));
    }
    
    protected void storeToForm(WorkflowAssignment wfAssignment, Map properties, boolean isTest, final String encryptedMnemonic, final boolean isMultiSig, final String accountSigners, final AccountInfo account) {
        String formDefId = getPropertyString("formDefId");
        
        if (formDefId != null && formDefId.trim().length() > 0) {
            ApplicationContext ac = AppUtil.getApplicationContext();
            AppService appService = (AppService) ac.getBean("appService");
            AppDefinition appDef = (AppDefinition) properties.get("appDef");

            String accountMnemonicField = getPropertyString("accountMnemonicField");
            String accountOwnerField = getPropertyString("accountOwnerField");
            String accountOwnerValue = WorkflowUtil.processVariable(getPropertyString("accountOwnerValue"), "", wfAssignment);
            String isTestAccountField = getPropertyString("isTestAccount");
            String isMultiSigAccountField = getPropertyString("isMultiSigAccount");
            String accountSignersField = getPropertyString("accountSigners");
            
            FormRowSet rowSet = new FormRowSet();
            
            FormRow row = new FormRow();
            
            //Account ID set as Record ID
            row.setId(account.accountId.toString());
            row = addRow(row, accountMnemonicField, encryptedMnemonic);
            row = addRow(row, accountOwnerField, accountOwnerValue);
            row = addRow(row, isTestAccountField, String.valueOf(isTest));
            row = addRow(row, isMultiSigAccountField, String.valueOf(isMultiSig));
            row = addRow(row, accountSignersField, isMultiSig ? accountSigners : "");

            rowSet.add(row);

            if (!rowSet.isEmpty()) {
                appService.storeFormData(appDef.getId(), appDef.getVersion().toString(), formDefId, rowSet, null);
            }
        }
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        
        return row;
    }
    
    protected void storeToWorkflowVariable(WorkflowAssignment wfAssignment, Map properties, boolean isTest, final TransactionReceipt receipt) {
        String wfResponseStatus = getPropertyString("wfResponseStatus");
        String wfIsTestAccount = getPropertyString("wfIsTestAccount");
        
        ApplicationContext ac = AppUtil.getApplicationContext();
        WorkflowManager workflowManager = (WorkflowManager) ac.getBean("workflowManager");
        
        storeValuetoActivityVar(workflowManager, wfAssignment.getActivityId(), wfResponseStatus, receipt.status.toString());
        storeValuetoActivityVar(workflowManager, wfAssignment.getActivityId(), wfIsTestAccount, String.valueOf(isTest));
    }
    
    private void storeValuetoActivityVar(WorkflowManager workflowManager, String activityId, String variable, String value) {
        if (!variable.isEmpty()) {
            workflowManager.activityVariable(activityId, variable, value);
        }
    }
}
