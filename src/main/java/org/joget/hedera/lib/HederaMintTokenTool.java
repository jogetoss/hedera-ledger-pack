package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenMintTransaction;
import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TokenType;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaProcessToolAbstract;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaMintTokenTool extends HederaProcessToolAbstract {

    @Override
    public String getName() {
        return "Hedera Mint Token Tool";
    }

    @Override
    public String getDescription() {
        return "Mint native tokens and NFTs on the Hedera DLT.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaMintTokenTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid(Map props) {
        //Do validations
        
        return true;
    }
    
    @Override
    protected Object runTool(Map props, Client client) 
            throws TimeoutException, PrecheckStatusException, BadMnemonicException, ReceiptStatusException {
        
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, primaryKey);
        
        FormRow row = rowSet.get(0);
        
        final String minterAccountId = row.getProperty(getPropertyString("minterAccountId"));
        final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
        final boolean mintMore = "mintMore".equalsIgnoreCase(getPropertyString("tokenIdHandling"));
        final boolean mintTypeNft = "nft".equalsIgnoreCase(getPropertyString("mintType"));
        
        final AccountId minterAccount = AccountId.fromString(minterAccountId);
        final PublicKey minterPublicKey = AccountUtil.derivePublicKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));
        final PrivateKey minterPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));
        
        TransactionRecord transactionRecord;
        
        if (mintMore) { // value is "mintMore"
            if (mintTypeNft) { 
                transactionRecord = mintMoreNft(row, null)
                        .freezeWith(client)
                        .sign(minterPrivateKey)
                        .execute(client)
                        .getRecord(client);
                
                String tokenId = row.getProperty(getPropertyString("tokenId"));
                
                storeNftDataToForm(props, row, tokenId, transactionRecord);
            } else {
                transactionRecord = mintMoreNativeToken(row, null)
                        .freezeWith(client)
                        .sign(minterPrivateKey)
                        .execute(client)
                        .getRecord(client);
            }
        } else { // value is "createNew"
            TokenCreateTransaction genericTokenCreateTransaction = createGenericToken(row, minterAccount, minterPublicKey);

            if (mintTypeNft) {
                TransactionRecord createTokenTxRecord = createAsNft(row, genericTokenCreateTransaction)
                        .freezeWith(client)
                        .sign(minterPrivateKey)
                        .execute(client)
                        .getRecord(client);
                
                storeTokenDataToForm(props, row, createTokenTxRecord);
                
                String tokenId = createTokenTxRecord.receipt.tokenId.toString();

                transactionRecord = mintMoreNft(row, tokenId)
                        .freezeWith(client)
                        .sign(minterPrivateKey)
                        .execute(client)
                        .getRecord(client);
                
                storeNftDataToForm(props, row, tokenId, transactionRecord);
            } else {
                transactionRecord = createAsNativeToken(row, genericTokenCreateTransaction)
                        .freezeWith(client)
                        .sign(minterPrivateKey)
                        .execute(client)
                        .getRecord(client);
                
                storeTokenDataToForm(props, row, transactionRecord);
            }
        }
        
        storeGenericTxDataToWorkflowVariable(props, transactionRecord);

        return transactionRecord;
    }
    
    private TokenCreateTransaction createGenericToken(FormRow row, AccountId minterAccount, PublicKey minterPubKey) {
        final String tokenName = row.getProperty(getPropertyString("tokenName"));
        final String tokenSymbol = row.getProperty(getPropertyString("tokenSymbol"));
        final String tokenMemo = row.getProperty(getPropertyString("tokenMemo"));
//        final String maxFeeAmount = row.getProperty(getPropertyString("maxFeeAmount"));

        //Look into custom fees features in the future
        return new TokenCreateTransaction()
            .setTokenName(tokenName)
            .setTokenSymbol(tokenSymbol)
            .setTokenMemo(tokenMemo) //A short publicly visible memo about the token.
            .setTreasuryAccountId(minterAccount) //This account will receive the specified initial supply and any additional tokens that are minted. If tokens are burned, the supply will decreased from the treasury account.
            .setAdminKey(minterPubKey) //The key which can perform token update and token delete operations on the token. The admin key has the authority to change the supply key, freeze key, pause key, wipe key, and KYC key. It can also update the treasury account of the token. If empty, the token can be perceived as immutable.
            .setFreezeKey(minterPubKey) //The key which can sign to freeze or unfreeze an account for token transactions. If empty, freezing is not possible.
            .setWipeKey(minterPubKey) //The key which can wipe the token balance of an account. If empty, wipe is not possible.
//            .setKycKey(minterPubKey) //The key which can grant or revoke KYC of an account for the token's transactions. If empty, KYC is not required, and KYC grant or revoke operations are not possible.
            .setPauseKey(minterPubKey) //The key that has the authority to pause or unpause a token. Pausing a token prevents the token from participating in all transactions.
            .setSupplyKey(minterPubKey) //The key which can change the total supply of a token. This key is used to authorize token mint and burn transactions. If this is left empty, minting/burning tokens is not possible.
            .setFreezeDefault(false);
//            .setMaxTransactionFee(Hbar.fromString(maxFeeAmount));
    }
    
    private TokenCreateTransaction createAsNft(FormRow row, TokenCreateTransaction tokenCreateTransaction) {
        return tokenCreateTransaction
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setInitialSupply(0)
                .setDecimals(0)
                .setSupplyType(TokenSupplyType.FINITE);
    }
    
    private TokenCreateTransaction createAsNativeToken(FormRow row, TokenCreateTransaction tokenCreateTransaction) {
        final String amountToMint = row.getProperty(getPropertyString("amountToMint"));
        final String tokenDecimals = row.getProperty(getPropertyString("tokenDecimals"));

        return tokenCreateTransaction
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setInitialSupply(Integer.parseInt(amountToMint))
                .setDecimals(Integer.parseInt(tokenDecimals))
                .setSupplyType(TokenSupplyType.INFINITE);
    }
    
    private TokenMintTransaction mintMoreNft(FormRow row, String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            tokenId = row.getProperty(getPropertyString("tokenId"));
        }
        
        // Typically looks like --> Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4
        // IPFS should contain the JSON that then contains the file CID
        final String ipfsCid = row.getProperty(getPropertyString("ipfsCid"));
        
//        final String nftFileName = row.getProperty(getPropertyString("nftFileName"));
//        final String nftFileType = getPropertyString("nftFileType"); // Check for other common supported mime types
        
        return new TokenMintTransaction()
                .setTokenId(TokenId.fromString(tokenId))
                .addMetadata(ipfsCid.getBytes());
    }
    
    private TokenMintTransaction mintMoreNativeToken(FormRow row, String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            tokenId = row.getProperty(getPropertyString("tokenId"));
        }
        
        final String additionalAmountToMint = row.getProperty(getPropertyString("additionalAmountToMint"));
        
        return new TokenMintTransaction()
                .setTokenId(TokenId.fromString(tokenId))
                .setAmount(Integer.parseInt(additionalAmountToMint));
    }
    
    private void storeTokenDataToForm(Map properties, FormRow row, TransactionRecord transactionRecord) {
        String formDefId = getPropertyString("formDefIdStoreTokenData");
        
        if (formDefId == null || formDefId.isEmpty()) {
            LogUtil.warn(getClassName(), "Unable to store new token data to form. Encountered blank form ID.");
            return;
        }
        
        String tokenName = row.getProperty(getPropertyString("tokenName"));
        String tokenSymbol = row.getProperty(getPropertyString("tokenSymbol"));
        String minterAccountId = row.getProperty(getPropertyString("minterAccountId"));
        boolean mintTypeNft = "nft".equalsIgnoreCase(getPropertyString("mintType"));
        
        final boolean isTest = BackendUtil.isTestnet(properties);
        
        String tokenTypeField = getPropertyString("tokenTypeField");
        String tokenNameField = getPropertyString("tokenNameField");
        String tokenSymbolField = getPropertyString("tokenSymbolField");
        String minterAccountField = getPropertyString("minterAccountField");
        String isTestnetField = getPropertyString("isTestnetField");
        
        FormRow newRow = new FormRow();

        //Token ID set as Record ID
        newRow.setId(transactionRecord.receipt.tokenId.toString());
        newRow = addRow(newRow, tokenTypeField, mintTypeNft ? "NFT" : "Native Token");
        newRow = addRow(newRow, tokenNameField, tokenName);
        newRow = addRow(newRow, tokenSymbolField, tokenSymbol);
        newRow = addRow(newRow, minterAccountField, minterAccountId);
        newRow = addRow(newRow, isTestnetField, String.valueOf(isTest));
        
        FormRowSet rowSet = new FormRowSet();
        rowSet.add(newRow);

        if (!rowSet.isEmpty()) {
            FormRowSet storedData = appService.storeFormData(appDef.getId(), appDef.getVersion().toString(), formDefId, rowSet, null);
            if (storedData == null) {
                LogUtil.warn(getClassName(), "Unable to store token data to form. Encountered invalid form ID of '" + formDefId + "'.");
            }
        }
    }
    
    private void storeNftDataToForm(Map properties, FormRow row, String tokenId, TransactionRecord transactionRecord) {
        String formDefId = getPropertyString("formDefIdStoreNftData");
        
        if (formDefId == null || formDefId.isEmpty()) {
            LogUtil.warn(getClassName(), "Unable to store new NFT data to form. Encountered blank form ID.");
            return;
        }
        
        String minterAccountId = row.getProperty(getPropertyString("minterAccountId"));
        
        final boolean isTest = BackendUtil.isTestnet(properties);
        
        String nftAssociatedTokenIdField = getPropertyString("nftAssociatedTokenIdField");
        String minterAccountField = getPropertyString("nftMinterAccountField");
        String isNftOnTestnetField = getPropertyString("isNftOnTestnetField");

        FormRow newRow = new FormRow();

        //NFT Serial Number set as Record ID
        newRow.setId(transactionRecord.receipt.serials.get(0).toString());
        newRow = addRow(newRow, nftAssociatedTokenIdField, tokenId);
        newRow = addRow(newRow, minterAccountField, minterAccountId);
        newRow = addRow(newRow, isNftOnTestnetField, String.valueOf(isTest));
        
        FormRowSet rowSet = new FormRowSet();
        rowSet.add(newRow);

        if (!rowSet.isEmpty()) {
            FormRowSet storedData = appService.storeFormData(appDef.getId(), appDef.getVersion().toString(), formDefId, rowSet, null);
            if (storedData == null) {
                LogUtil.warn(getClassName(), "Unable to store NFT data to form. Encountered invalid form ID of '" + formDefId + "'.");
            }
        }
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        
        return row;
    }
}
