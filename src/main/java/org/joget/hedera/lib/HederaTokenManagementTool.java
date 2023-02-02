package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenDissociateTransaction;
import com.hedera.hashgraph.sdk.TokenGrantKycTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenRevokeKycTransaction;
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

public class HederaTokenManagementTool extends HederaProcessToolAbstract {
    
    @Override
    public String getName() {
        return "Hedera Token Management Tool";
    }

    @Override
    public String getDescription() {
        return "Performs various functions offered by Hedera Token Service, for tokens on the Hedera DLT.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaTokenManagementTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid(Map props) {
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = getFormRecord(formDefId, null);
        
        if (rowSet == null || rowSet.isEmpty()) {
            LogUtil.warn(getClassName(), "No record found with record ID '" + primaryKey + "' from this form '" + formDefId + "'.");
            return false;
        }
        
        return true;
    }
    
    @Override
    protected Object runTool(Map props, Client client) 
            throws TimeoutException, RuntimeException {
        
        try {
            String formDefId = getPropertyString("formDefId");

            FormRowSet rowSet = getFormRecord(formDefId, null);

            FormRow row = rowSet.get(0);
            
            final String tokenId = row.getProperty(getPropertyString("tokenId"));

            final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
            
            TransactionRecord transactionRecord;

            final OperationType operationType = OperationType.fromString(getPropertyString("operationType"));

            switch (operationType) {
                case ASSOCIATE: {
                    final PrivateKey targetAccountPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(
                            Mnemonic.fromString(
                                    PluginUtil.decrypt(
                                            WorkflowUtil.processVariable(getPropertyString("targetAccountMnemonic"), "", wfAssignment)
                                    )
                            )
                    );
                    
                    transactionRecord = new TokenAssociateTransaction()
                        .setAccountId(targetAccount)
                        .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId)))
                        .freezeWith(client)
                        .sign(targetAccountPrivateKey)
                        .execute(client)
                        .getRecord(client);
                    break;
                }
                case DISSOCIATE: {
                    final PrivateKey targetAccountPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(
                            Mnemonic.fromString(
                                    PluginUtil.decrypt(
                                            WorkflowUtil.processVariable(getPropertyString("targetAccountMnemonic"), "", wfAssignment)
                                    )
                            )
                    );
                    
                    transactionRecord = new TokenDissociateTransaction()
                        .setAccountId(targetAccount)
                        .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId)))
                        .freezeWith(client)
                        .sign(targetAccountPrivateKey)
                        .execute(client)
                        .getRecord(client);
                    break;
                }
                case GRANT_KYC: {
                    final PrivateKey kycAccountPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(
                            Mnemonic.fromString(
                                    PluginUtil.decrypt(
                                            WorkflowUtil.processVariable(getPropertyString("kycAccountMnemonic"), "", wfAssignment)
                                    )
                            )
                    );
                    
                    transactionRecord = new TokenGrantKycTransaction()
                            .setAccountId(targetAccount)
                            .setTokenId(TokenId.fromString(tokenId))
                            .freezeWith(client)
                            .sign(kycAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case REVOKE_KYC: {
                    final PrivateKey kycAccountPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(
                            Mnemonic.fromString(
                                    PluginUtil.decrypt(
                                            WorkflowUtil.processVariable(getPropertyString("kycAccountMnemonic"), "", wfAssignment)
                                    )
                            )
                    );
                    
                    transactionRecord = new TokenRevokeKycTransaction()
                            .setAccountId(targetAccount)
                            .setTokenId(TokenId.fromString(tokenId))
                            .freezeWith(client)
                            .sign(kycAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                default:
                    LogUtil.warn(getClassName(), "Unknown token management operation type!");
                    return null;
            }

            storeGenericTxDataToWorkflowVariable(props, transactionRecord);

            return transactionRecord;
        } catch (PrecheckStatusException | BadMnemonicException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    private enum OperationType {
        ASSOCIATE("associate"),
        DISSOCIATE("dissociate"),
        GRANT_KYC("grantKyc"),
        REVOKE_KYC("revokeKyc");
        
        private final String value;
        
        OperationType(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static OperationType fromString(String text) {
            for (OperationType operation : OperationType.values()) {
                if ((operation.value).equalsIgnoreCase(text)) {
                    return operation;
                }
            }

            return null;
        }
    }
}
