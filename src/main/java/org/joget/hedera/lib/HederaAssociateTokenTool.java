package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenDissociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaProcessToolAbstract;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaAssociateTokenTool extends HederaProcessToolAbstract {
    
    @Override
    public String getName() {
        return "Hedera Associate Token Tool";
    }

    @Override
    public String getDescription() {
        return "Associate/Dissociate with tokens for accounts on the Hedera DLT.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaAssociateTokenTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid(Map props) {
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = getFormRecord(formDefId, null);
        
        if (rowSet == null || rowSet.isEmpty()) {
            LogUtil.warn(getClassName(), "Token " + getPropertyString("operationType") + " aborted. No record found with record ID '" + primaryKey + "' from this form '" + formDefId + "'.");
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean isInputDataValidWithClient(Map props, Client client) 
            throws TimeoutException, RuntimeException {
        
        try {
            String formDefId = getPropertyString("formDefId");

            FormRowSet rowSet = getFormRecord(formDefId, null);

            FormRow row = rowSet.get(0);

            final String accountId = row.getProperty(getPropertyString("accountId"));
            final AccountId account = AccountId.fromString(accountId);

            final String operationType = getPropertyString("operationType"); //"associate" or "dissociate"
            final String tokenId = row.getProperty(getPropertyString("tokenId"));

            AccountBalance accountBalances = new AccountBalanceQuery()
                    .setAccountId(account)
                    .execute(client);

            Map<TokenId, Long> tokenMap = accountBalances.tokens;
            Long tokenBalance = null;

            if (!tokenMap.isEmpty()) {
                tokenBalance = tokenMap.get(TokenId.fromString(tokenId));
            }

            if ("associate".equalsIgnoreCase(operationType)) {
                if (tokenBalance != null) {
                    LogUtil.warn(getClassName(), "Token " + operationType + " aborted. Account is already associated with token ID of " + tokenId);
                    return false;
                }
            } else {
                if (tokenBalance != null && !tokenBalance.equals(0L)) {
                    LogUtil.warn(getClassName(), "Token " + operationType + " aborted. Account with token ID of " + tokenId + " does not have a zero balance!");
                    return false;
                }
                if (tokenBalance == null) {
                    LogUtil.warn(getClassName(), "Token " + operationType + " aborted. Account is already dissociated with token ID of " + tokenId);
                    return false;
                }
            }

            return true;
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    @Override
    protected Object runTool(Map props, Client client) 
            throws TimeoutException, RuntimeException {
        
        try {
            String formDefId = getPropertyString("formDefId");

            FormRowSet rowSet = getFormRecord(formDefId, null);

            FormRow row = rowSet.get(0);

            final String accountId = row.getProperty(getPropertyString("accountId"));
            final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));

            final AccountId account = AccountId.fromString(accountId);
            final PrivateKey accountPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));

            TransactionRecord transactionRecord;

            final String operationType = getPropertyString("operationType"); //"associate" or "dissociate"
            final String tokenId = row.getProperty(getPropertyString("tokenId"));

            if ("associate".equalsIgnoreCase(operationType)) {
                transactionRecord = new TokenAssociateTransaction()
                        .setAccountId(account)
                        .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId)))
                        .freezeWith(client)
                        .sign(accountPrivateKey)
                        .execute(client)
                        .getRecord(client);
            } else {
                transactionRecord = new TokenDissociateTransaction()
                        .setAccountId(account)
                        .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId)))
                        .freezeWith(client)
                        .sign(accountPrivateKey)
                        .execute(client)
                        .getRecord(client);
            }

            storeGenericTxDataToWorkflowVariable(props, transactionRecord);

            return transactionRecord;
        } catch (PrecheckStatusException | BadMnemonicException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
}
