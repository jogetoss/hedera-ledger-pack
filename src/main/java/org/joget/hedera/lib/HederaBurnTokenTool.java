package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.NftId;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TokenBurnTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TokenNftInfo;
import com.hedera.hashgraph.sdk.TokenNftInfoQuery;
import com.hedera.hashgraph.sdk.TokenType;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaProcessToolAbstract;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaBurnTokenTool extends HederaProcessToolAbstract {
    
    @Override
    public String getName() {
        return "Hedera Burn Token Tool";
    }

    @Override
    public String getDescription() {
        return "Burn fungible and non-fungible tokens that was previously minted on the Hedera DLT.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaBurnTokenTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid(Map props) {
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = getFormRecord(formDefId, null);
        
        if (rowSet == null || rowSet.isEmpty()) {
            LogUtil.warn(getClassName(), "Burn transaction aborted. No record found with record ID '" + primaryKey + "' from this form '" + formDefId + "'.");
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

            final boolean burnTypeNft = "nft".equalsIgnoreCase(getPropertyString("burnType"));
            final String tokenId = row.getProperty(getPropertyString("tokenId"));

            TokenInfo tokenInfo = new TokenInfoQuery()
                    .setTokenId(TokenId.fromString(tokenId))
                    .execute(client);

            final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
            final PublicKey minterPublicKey = AccountUtil.derivePublicKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));

            if (tokenInfo == null) {
                LogUtil.warn(getClassName(), "Burn transaction aborted. Specified token ID of " + tokenId + " does not exist.");
                return false;
            }

            if (tokenInfo.supplyKey == null) {
                LogUtil.warn(getClassName(), "Burn transaction aborted. Specified token ID of " + tokenId + " does not have a supply key set.");
                return false;
            }

            if (!(tokenInfo.supplyKey.toString()).equals(minterPublicKey.toString())) {
                LogUtil.warn(getClassName(), "Burn transaction aborted. Specified token ID of " + tokenId + " - supply key not authorized for account.");
                return false;
            }

            if (burnTypeNft) {
                if (!(tokenInfo.tokenType).equals(TokenType.NON_FUNGIBLE_UNIQUE)) {
                    LogUtil.warn(getClassName(), "Burn transaction aborted. Specified token ID of " + tokenId + " is not an NFT.");
                    return false;
                }

                final String nftSerialNumber = row.getProperty(getPropertyString("nftSerialNumber"));

                List<TokenNftInfo> tokenNftInfo = new TokenNftInfoQuery()
                        .setNftId(new NftId(TokenId.fromString(tokenId), Long.parseLong(nftSerialNumber)))
                        .execute(client);

                if (tokenNftInfo == null || tokenNftInfo.isEmpty()) {
                    LogUtil.warn(getClassName(), "Burn transaction aborted. Specified token ID of " + tokenId + " - NFT serial number of " + nftSerialNumber + " does not exist.");
                    return false;
                }
            } else {
                if (!(tokenInfo.tokenType).equals(TokenType.FUNGIBLE_COMMON)) {
                    LogUtil.warn(getClassName(), "Burn transaction aborted. Specified token ID of " + tokenId + " is not a fungible token.");
                    return false;
                }

                final String amountToBurn = row.getProperty(getPropertyString("amountToBurn"));

                if (Long.compare(tokenInfo.totalSupply, Long.parseLong(amountToBurn)) < 0) {
                    LogUtil.warn(getClassName(), "Burn transaction aborted. Specified token ID of " + tokenId + " - specified burn amount exceeded token supply.");
                    return false;
                }
            }

            return true;
        } catch (PrecheckStatusException | BadMnemonicException e) {
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

            final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
            final boolean burnTypeNft = "nft".equalsIgnoreCase(getPropertyString("burnType"));
            final String tokenId = row.getProperty(getPropertyString("tokenId"));

            final PrivateKey accountPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));

            TransactionRecord transactionRecord;

            TokenBurnTransaction tokenBurnTx = new TokenBurnTransaction()
                .setTokenId(TokenId.fromString(tokenId));

            if (burnTypeNft) {
                final String nftSerialNumber = row.getProperty(getPropertyString("nftSerialNumber"));

                tokenBurnTx.addSerial(Long.parseLong(nftSerialNumber));
            } else {
                final String amountToBurn = row.getProperty(getPropertyString("amountToBurn"));

                TokenInfo tokenInfo = new TokenInfoQuery()
                        .setTokenId(TokenId.fromString(tokenId))
                        .execute(client);

                tokenBurnTx.setAmount(TransactionUtil.calcActualTokenAmountBasedOnDecimals(amountToBurn, tokenInfo.decimals));
            }

            transactionRecord = tokenBurnTx
                    .freezeWith(client)
                    .sign(accountPrivateKey)
                    .execute(client)
                    .getRecord(client);

            storeGenericTxDataToWorkflowVariable(props, transactionRecord);

            return transactionRecord;
        } catch (PrecheckStatusException | BadMnemonicException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
}
