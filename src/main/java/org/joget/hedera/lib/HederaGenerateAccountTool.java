package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import java.util.Map;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

public class HederaGenerateAccountTool extends DefaultApplicationPlugin {

    @Override
    public String getName() {
        return "Hedera Generate Account Tool";
    }

    @Override
    public String getVersion() {
        return PluginUtil.getProjectVersion(this.getClass());
    }

    @Override
    public String getDescription() {
        return "Generates a new account on the Hedera DLT.";
    }

    @Override
    public Object execute(Map props) {
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");
        
        final String operatorId = getPropertyString("operatorId");
        final String operatorKey = getPropertyString("operatorKey");
        final String networkType = getPropertyString("networkType");
        final String fundTestAccountFlag = getPropertyString("fundTestAccount");
        final String enableMultiSigFlag = getPropertyString("enableMultiSig");
        final String formDefIdGetData = getPropertyString("formDefIdGetData");
        final String getMnemonicField = getPropertyString("getMnemonicField");
        final String accountIdsToSign = WorkflowUtil.processVariable(getPropertyString("accountIdsToSign"), "", wfAssignment);
        boolean isTest = false;
        boolean isMultiSig = false;
        
        if ((BackendUtil.TESTNET_NAME).equals(networkType) || (BackendUtil.PREVIEWNET_NAME).equals(networkType)) {
            isTest = true;
        }
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        try {
            AccountId newAccountId = null;
            final Client client = BackendUtil.getHederaClient(operatorId, operatorKey, networkType);
            
            if (client != null) {
                String encryptedMnemonic = "";
                String accountSigners = "";
                AccountCreateTransaction newAccountTransaction = new AccountCreateTransaction();
                
                if ("true".equals(enableMultiSigFlag)) {
                    isMultiSig = true;
                    
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
                
                if (isTest && "true".equals(fundTestAccountFlag)) {
                    fundTestAccount(newAccountTransaction);
                }

                TransactionResponse transactionResponse = newAccountTransaction.execute(client);

                // This will wait for the tx to complete and get a "receipt" as response. Fyi there is also async methods available if needed.
                TransactionReceipt receipt = transactionResponse.getReceipt(client);

                newAccountId = receipt.accountId;

                AccountInfo newAccountInfo = new AccountInfoQuery()
                .setAccountId(newAccountId)
                .execute(client);

                storeToForm(wfAssignment, props, isTest, encryptedMnemonic, isMultiSig, accountSigners, newAccountInfo);
                storeToWorkflowVariable(wfAssignment, props, isTest, receipt);
            }
            
            return newAccountId;
        } catch (Exception ex) {
            LogUtil.error(getClass().getName(), ex, "Error executing plugin...");
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
    
    private void fundTestAccount(AccountCreateTransaction newAccountTransaction) {
        // Can make the initial balance a configurable plugin property if you want to
        newAccountTransaction.setInitialBalance(Hbar.from(20));
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

            if (rowSet.size() > 0) {
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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/HederaGenerateAccountTool.json", null, true, "messages/HederaMessages");
    }
}
