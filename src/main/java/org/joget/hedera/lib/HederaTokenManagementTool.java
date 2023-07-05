package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenBurnTransaction;
import com.hedera.hashgraph.sdk.TokenDeleteTransaction;
import com.hedera.hashgraph.sdk.TokenDissociateTransaction;
import com.hedera.hashgraph.sdk.TokenFreezeTransaction;
import com.hedera.hashgraph.sdk.TokenGrantKycTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenPauseTransaction;
import com.hedera.hashgraph.sdk.TokenRevokeKycTransaction;
import com.hedera.hashgraph.sdk.TokenUnfreezeTransaction;
import com.hedera.hashgraph.sdk.TokenUnpauseTransaction;
import com.hedera.hashgraph.sdk.TokenWipeTransaction;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaProcessTool;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.MirrorRestService;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class HederaTokenManagementTool extends HederaProcessTool {
    
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
    protected Object runTool(Map props) 
            throws TimeoutException, RuntimeException {
        
        try {
            FormRow row = getFormRecord(getPropertyString("formDefId"), null).get(0);
            
            final String tokenId = row.getProperty(getPropertyString("tokenId"));
            
            final String transactionMemo = WorkflowUtil.processVariable(getPropertyString("transactionMemo"), "", wfAssignment).trim();
            
            TransactionRecord transactionRecord;
            final OperationType operationType = OperationType.fromString(getPropertyString("operationType"));

            switch (operationType) {
                case ASSOCIATE: {
                    final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
                    final PrivateKey targetAccountPrivateKey = getPrivateKey(getPropertyString("targetAccountMnemonic"));
                    
                    transactionRecord = new TokenAssociateTransaction()
                            .setAccountId(targetAccount)
                            .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId)))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(targetAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case DISSOCIATE: {
                    final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
                    final PrivateKey targetAccountPrivateKey = getPrivateKey(getPropertyString("targetAccountMnemonic"));
                    
                    transactionRecord = new TokenDissociateTransaction()
                            .setAccountId(targetAccount)
                            .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId)))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(targetAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case GRANT_KYC: {
                    final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
                    final PrivateKey kycAccountPrivateKey = getPrivateKey(getPropertyString("kycAccountMnemonic"));
                    
                    transactionRecord = new TokenGrantKycTransaction()
                            .setAccountId(targetAccount)
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(kycAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case REVOKE_KYC: {
                    final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
                    final PrivateKey kycAccountPrivateKey = getPrivateKey(getPropertyString("kycAccountMnemonic"));
                    
                    transactionRecord = new TokenRevokeKycTransaction()
                            .setAccountId(targetAccount)
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(kycAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case FREEZE: {
                    final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
                    final PrivateKey freezeAccountPrivateKey = getPrivateKey(getPropertyString("freezeAccountMnemonic"));
                    
                    transactionRecord = new TokenFreezeTransaction()
                            .setAccountId(targetAccount)
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(freezeAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case UNFREEZE: {
                    final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
                    final PrivateKey freezeAccountPrivateKey = getPrivateKey(getPropertyString("freezeAccountMnemonic"));
                    
                    transactionRecord = new TokenUnfreezeTransaction()
                            .setAccountId(targetAccount)
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(freezeAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case WIPE: {
                    final AccountId targetAccount = AccountId.fromString(row.getProperty(getPropertyString("targetAccount")));
                    final PrivateKey wipeAccountPrivateKey = getPrivateKey(getPropertyString("wipeAccountMnemonic"));
                    final String wipeTokenType = getPropertyString("wipeTokenType"); //"fungibleToken" or "nft"
                    
                    TokenWipeTransaction tokenWipeTx = new TokenWipeTransaction()
                            .setAccountId(targetAccount)
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo);
                    
                    if (wipeTokenType.equals("fungibleToken")) {
                        final String amountToWipe = row.getProperty(getPropertyString("amountToWipe"));
                        
                        tokenWipeTx.setAmount(Long.parseLong(amountToWipe));
                    } else {
                        final String nftSerialNumberToWipe = row.getProperty(getPropertyString("nftSerialNumberToWipe"));
                        
                        tokenWipeTx.addSerial(Long.parseLong(nftSerialNumberToWipe));
                    }
                    
                    transactionRecord = tokenWipeTx
                            .freezeWith(client)
                            .sign(wipeAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case PAUSE: {
                    final PrivateKey pauseAccountPrivateKey = getPrivateKey(getPropertyString("pauseAccountMnemonic"));
                    
                    transactionRecord = new TokenPauseTransaction()
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(pauseAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case UNPAUSE: {
                    final PrivateKey pauseAccountPrivateKey = getPrivateKey(getPropertyString("pauseAccountMnemonic"));
                    
                    transactionRecord = new TokenUnpauseTransaction()
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(pauseAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case DELETE: {
                    final PrivateKey adminAccountPrivateKey = getPrivateKey(getPropertyString("adminAccountMnemonic"));
                    
                    transactionRecord = new TokenDeleteTransaction()
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo)
                            .freezeWith(client)
                            .sign(adminAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                case BURN: {
                    final PrivateKey supplyAccountPrivateKey = getPrivateKey(getPropertyString("supplyAccountMnemonic"));
                    final String burnTokenType = getPropertyString("burnTokenType"); //"fungibleToken" or "nft"
                    
                    TokenBurnTransaction tokenBurnTx = new TokenBurnTransaction()
                            .setTokenId(TokenId.fromString(tokenId))
                            .setTransactionMemo(transactionMemo);
                    
                    if (burnTokenType.equals("fungibleToken")) {
                        final String amountToBurn = row.getProperty(getPropertyString("amountToBurn"));
                        
                        final MirrorRestService restService = new MirrorRestService(props, client.getLedgerId());
                        JSONObject jsonResponse = restService.get("tokens/" + tokenId);
                        
                        tokenBurnTx.setAmount(TransactionUtil.calcActualTokenAmountBasedOnDecimals(amountToBurn, Integer.parseInt(jsonResponse.getString("decimals"))));
                    } else {
                        final String nftSerialNumberToBurn = row.getProperty(getPropertyString("nftSerialNumberToBurn"));
                        
                        tokenBurnTx.addSerial(Long.parseLong(nftSerialNumberToBurn));
                    }
                    
                    transactionRecord = tokenBurnTx
                            .freezeWith(client)
                            .sign(supplyAccountPrivateKey)
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                default:
                    LogUtil.warn(getClassName(), "Unknown token management operation type!");
                    return null;
            }

            storeGenericTxDataToWorkflowVariable(transactionRecord);

            return transactionRecord;
        } catch (PrecheckStatusException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    private PrivateKey getPrivateKey(String mnemonicString) {
        try {
            return AccountUtil.derivePrivateKeyFromMnemonic(
                    Mnemonic.fromString(
                            PluginUtil.decrypt(
                                    WorkflowUtil.processVariable(mnemonicString, "", wfAssignment)
                            )
                    )
            );
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Unable to derive private key from mnemonic...");
            return null;
        }
    }
    
    private enum OperationType {
        ASSOCIATE("associate"),
        DISSOCIATE("dissociate"),
        GRANT_KYC("grantKyc"),
        REVOKE_KYC("revokeKyc"),
        FREEZE("freeze"),
        UNFREEZE("unfreeze"),
        WIPE("wipe"),
        PAUSE("pause"),
        UNPAUSE("unpause"),
        DELETE("delete"),
        BURN("burn");
        
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
