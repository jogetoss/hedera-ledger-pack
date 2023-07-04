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
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
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
import org.joget.hedera.model.HederaProcessTool;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaMintTokenTool extends HederaProcessTool {

    @Override
    public String getName() {
        return "Hedera Mint Token Tool";
    }

    @Override
    public String getDescription() {
        return "Mint fungible and non-fungible tokens on the Hedera DLT.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaMintTokenTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid(Map props) {
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = getFormRecord(formDefId, null);
        
        if (rowSet == null || rowSet.isEmpty()) {
            LogUtil.warn(getClassName(), "Mint transaction aborted. No record found with record ID '" + primaryKey + "' from this form '" + formDefId + "'.");
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

            final boolean mintMore = "mintMore".equalsIgnoreCase(getPropertyString("tokenIdHandling"));
            final boolean mintTypeNft = "nft".equalsIgnoreCase(getPropertyString("mintType"));

            if (mintMore) {
                final String tokenId = row.getProperty(getPropertyString("tokenId"));

                TokenInfo tokenInfo = new TokenInfoQuery()
                    .setTokenId(TokenId.fromString(tokenId))
                    .execute(client);

                final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
                final PublicKey minterPublicKey = AccountUtil.derivePublicKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));

                if (tokenInfo.supplyKey != null && !(tokenInfo.supplyKey.toString()).equals(minterPublicKey.toString())) {
                    LogUtil.warn(getClassName(), "Mint transaction aborted. Specified token ID of " + tokenId + " - supply key not authorized for minter account.");
                    return false;
                }

                if (mintTypeNft) {
                    if (!(tokenInfo.tokenType).equals(TokenType.NON_FUNGIBLE_UNIQUE)) {
                        LogUtil.warn(getClassName(), "Mint transaction aborted. Specified token ID of " + tokenId + " is not an NFT.");
                        return false;
                    }
                } else {
                    if (!(tokenInfo.tokenType).equals(TokenType.FUNGIBLE_COMMON)) {
                        LogUtil.warn(getClassName(), "Mint transaction aborted. Specified token ID of " + tokenId + " is not a fungible token.");
                        return false;
                    }
                }
            }
        
            return true;
        } catch (PrecheckStatusException | BadMnemonicException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    @Override
    protected Object runTool(Map props) 
            throws TimeoutException, RuntimeException {
        
        try {
            String formDefId = getPropertyString("formDefId");

            FormRowSet rowSet = getFormRecord(formDefId, null);

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

                    storeNftDataToForm(row, tokenId, transactionRecord);
                } else {
                    transactionRecord = mintMoreNativeToken(client, row, null)
                            .freezeWith(client)
                            .sign(minterPrivateKey)
                            .execute(client)
                            .getRecord(client);
                }
            } else { // value is "createNew"
                TokenCreateTransaction genericTokenCreateTx = createGenericToken(row, minterAccount, minterPublicKey, client.getOperatorPublicKey());

                genericTokenCreateTx = setTokenMaxSupply(row, genericTokenCreateTx);

                if (mintTypeNft) {
                    TransactionRecord createTokenTxRecord = createAsNft(row, genericTokenCreateTx)
                            .freezeWith(client)
                            .sign(minterPrivateKey)
                            .execute(client)
                            .getRecord(client);

                    storeTokenDataToForm(row, createTokenTxRecord);

                    String tokenId = createTokenTxRecord.receipt.tokenId.toString();

                    transactionRecord = mintMoreNft(row, tokenId)
                            .freezeWith(client)
                            .sign(minterPrivateKey)
                            .execute(client)
                            .getRecord(client);

                    storeNftDataToForm(row, tokenId, transactionRecord);
                } else {
                    transactionRecord = createAsNativeToken(row, genericTokenCreateTx)
                            .freezeWith(client)
                            .sign(minterPrivateKey)
                            .execute(client)
                            .getRecord(client);

                    storeTokenDataToForm(row, transactionRecord);
                }
            }

            storeGenericTxDataToWorkflowVariable(transactionRecord);

            return transactionRecord;
        } catch (PrecheckStatusException | BadMnemonicException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    private TokenCreateTransaction createGenericToken(FormRow row, AccountId minterAccount, PublicKey minterPubKey, PublicKey operatorPubKey) {
        final String tokenName = row.getProperty(getPropertyString("tokenName"));
        final String tokenSymbol = row.getProperty(getPropertyString("tokenSymbol"));
        final String tokenMemo = row.getProperty(getPropertyString("tokenMemo"));
//        final String maxFeeAmount = row.getProperty(getPropertyString("maxFeeAmount")); //Look into custom fees features in the future

        final String adminKey = getPropertyString("adminKey");
        final String freezeKey = getPropertyString("freezeKey");
        final String wipeKey = getPropertyString("wipeKey");
        final String kycKey = getPropertyString("kycKey");
        final String pauseKey = getPropertyString("pauseKey");
        final String supplyKey = getPropertyString("supplyKey");
        final String feeScheduleKey = getPropertyString("feeScheduleKey");
        
        TokenCreateTransaction tokenCreateTx = new TokenCreateTransaction()
            .setTokenName(tokenName)
            .setTokenSymbol(tokenSymbol)
            .setTokenMemo(tokenMemo) //A short publicly visible memo about the token.
            /*  
                This account will receive the specified initial supply and any additional tokens that are minted. 
                If tokens are burned, the supply will decreased from the treasury account.
            */
            .setTreasuryAccountId(minterAccount)
            .setFreezeDefault(false);
            
            /*
                The key which can perform token update and token delete operations on the token. 
                The admin key has the authority to change the supply key, freeze key, pause key, wipe key, and KYC key. 
                It can also update the treasury account of the token. If empty, the token can be perceived as immutable.
            */
            switch (adminKey) {
                case "operator":
                    tokenCreateTx.setAdminKey(operatorPubKey);
                    break;
                case "minter":
                    tokenCreateTx.setAdminKey(minterPubKey);
                    break;
                default:
                    break;
            }
                
            /* 
                The key which can sign to freeze or unfreeze an account for token transactions. 
                If empty, freezing is not possible.
            */
            switch (freezeKey) {
                case "operator":
                    tokenCreateTx.setFreezeKey(operatorPubKey);
                    break;
                case "minter":
                    tokenCreateTx.setFreezeKey(minterPubKey);
                    break;
                default:
                    break;
            }
                
            //The key which can wipe the token balance of an account. If empty, wipe is not possible.
            switch (wipeKey) {
                case "operator":
                    tokenCreateTx.setWipeKey(operatorPubKey);
                    break;
                case "minter":
                    tokenCreateTx.setWipeKey(minterPubKey);
                    break;
                default:
                    break;
            }
                
            /*
                The key which can grant or revoke KYC of an account for the token's transactions. 
                If empty, KYC is not required, and KYC grant or revoke operations are not possible.
            */
            switch (kycKey) {
                case "operator":
                    tokenCreateTx.setKycKey(operatorPubKey);
                    break;
                case "minter":
                    tokenCreateTx.setKycKey(minterPubKey);
                    break;
                default:
                    break;
            }
                
            /*
                The key that has the authority to pause or unpause a token. 
                Pausing a token prevents the token from participating in all transactions.
            */
            switch (pauseKey) {
                case "operator":
                    tokenCreateTx.setPauseKey(operatorPubKey);
                    break;
                case "minter":
                    tokenCreateTx.setPauseKey(minterPubKey);
                    break;
                default:
                    break;
            }
                
            /*
                The key which can change the total supply of a token. 
                This key is used to authorize token mint and burn transactions. 
                If this is left empty, minting/burning tokens is not possible.
            */
            switch (supplyKey) {
                case "operator":
                    tokenCreateTx.setSupplyKey(operatorPubKey);
                    break;
                case "minter":
                    tokenCreateTx.setSupplyKey(minterPubKey);
                    break;
                default:
                    break;
            }
            
            /*
                The key that can change the token's custom fee schedule. 
                A custom fee schedule token without a fee schedule key is immutable.
            */
            switch (feeScheduleKey) {
                case "operator":
                    tokenCreateTx.setFeeScheduleKey(operatorPubKey);
                    break;
                case "minter":
                    tokenCreateTx.setFeeScheduleKey(minterPubKey);
                    break;
                default:
                    break;
            }

//            .setMaxTransactionFee(Hbar.fromString(maxFeeAmount));

        return tokenCreateTx;
    }
    
    private TokenCreateTransaction setTokenMaxSupply(FormRow row, TokenCreateTransaction genericTokenCreateTx) {
        final String maxSupply = row.getProperty(getPropertyString("maxSupply"));
        final boolean mintTypeNft = "nft".equalsIgnoreCase(getPropertyString("mintType"));
        final String tokenDecimals = row.getProperty(getPropertyString("tokenDecimals"));
        
        //Set max supply for token. For NFT, defines how many serial numbers can exist for a token.
        if (maxSupply != null && !maxSupply.isBlank()) {
            genericTokenCreateTx.setSupplyType(TokenSupplyType.FINITE);
            
            if (mintTypeNft) {
                genericTokenCreateTx.setMaxSupply(Integer.parseInt(maxSupply));
            } else {
                final int maxSupplyInt = TransactionUtil.calcActualTokenAmountBasedOnDecimals(maxSupply, Integer.parseInt(tokenDecimals));
                genericTokenCreateTx.setMaxSupply(maxSupplyInt);
            }
        } else {
            genericTokenCreateTx.setSupplyType(TokenSupplyType.INFINITE);
        }
        
        return genericTokenCreateTx;
    }
    
    private TokenCreateTransaction createAsNft(FormRow row, TokenCreateTransaction genericTokenCreateTx) {
        return genericTokenCreateTx
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setInitialSupply(0)
                .setDecimals(0);
    }
    
    private TokenCreateTransaction createAsNativeToken(FormRow row, TokenCreateTransaction genericTokenCreateTx) {
        final String amountToMint = row.getProperty(getPropertyString("amountToMint"));
        final String tokenDecimals = row.getProperty(getPropertyString("tokenDecimals"));
        
        final int tokenDecimalsInt = Integer.parseInt(tokenDecimals);
        
        final int amountToMintInt = TransactionUtil.calcActualTokenAmountBasedOnDecimals(amountToMint, tokenDecimalsInt);
        
        return genericTokenCreateTx
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setInitialSupply(amountToMintInt)
                .setDecimals(tokenDecimalsInt);
    }
    
    private TokenMintTransaction mintMoreNft(FormRow row, String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            tokenId = row.getProperty(getPropertyString("tokenId"));
        }
        
        // Typically looks like --> Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4
        // IPFS should contain the JSON that then contains the file CID
        String ipfsCid = row.getProperty(getPropertyString("ipfsCid"));

        //Workaround to not duplicate plugin property id
        if (ipfsCid == null) {
            ipfsCid = row.getProperty(getPropertyString("ipfsCidMintMore"));
        }
        
//        final String nftFileName = row.getProperty(getPropertyString("nftFileName"));
//        final String nftFileType = getPropertyString("nftFileType"); // Check for other common supported mime types
        
        return new TokenMintTransaction()
                .setTokenId(TokenId.fromString(tokenId))
                .addMetadata(ipfsCid.getBytes());
    }
    
    private TokenMintTransaction mintMoreNativeToken(Client client, FormRow row, String tokenId) 
            throws TimeoutException, RuntimeException {
        
        try {
            if (tokenId == null || tokenId.isBlank()) {
                tokenId = row.getProperty(getPropertyString("tokenId"));
            }

            final String additionalAmountToMint = row.getProperty(getPropertyString("additionalAmountToMint"));

            TokenInfo tokenInfo = new TokenInfoQuery()
                    .setTokenId(TokenId.fromString(tokenId))
                    .execute(client);

            final int additionalAmountToMintInt = TransactionUtil.calcActualTokenAmountBasedOnDecimals(additionalAmountToMint, tokenInfo.decimals);

            return new TokenMintTransaction()
                    .setTokenId(TokenId.fromString(tokenId))
                    .setAmount(additionalAmountToMintInt);
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        } 
    }
    
    private void storeTokenDataToForm(FormRow row, TransactionRecord transactionRecord) {
        String formDefId = getPropertyString("formDefIdStoreTokenData");
        
        if (formDefId == null || formDefId.isEmpty()) {
            LogUtil.warn(getClassName(), "Unable to store new token data to form. Encountered blank form ID.");
            return;
        }
        
        String tokenName = row.getProperty(getPropertyString("tokenName"));
        String tokenSymbol = row.getProperty(getPropertyString("tokenSymbol"));
        String minterAccountId = row.getProperty(getPropertyString("minterAccountId"));
        boolean mintTypeNft = "nft".equalsIgnoreCase(getPropertyString("mintType"));
        
        final boolean isTest = client.getLedgerId().isTestnet() || client.getLedgerId().isPreviewnet();
        
        String tokenTypeField = getPropertyString("tokenTypeField");
        String tokenNameField = getPropertyString("tokenNameField");
        String tokenSymbolField = getPropertyString("tokenSymbolField");
        String minterAccountField = getPropertyString("minterAccountField");
        String isTestnetField = getPropertyString("isTestnetField");
        
        FormRow newRow = new FormRow();

        //Token ID set as Record ID
        newRow.setId(transactionRecord.receipt.tokenId.toString());
        newRow = addRow(newRow, tokenTypeField, mintTypeNft ? "Non-fungible Token" : "Fungible Token");
        newRow = addRow(newRow, tokenNameField, tokenName);
        newRow = addRow(newRow, tokenSymbolField, tokenSymbol);
        newRow = addRow(newRow, minterAccountField, minterAccountId);
        newRow = addRow(newRow, isTestnetField, String.valueOf(isTest));
        
        FormRowSet storedData = storeFormRow(formDefId, newRow);
        if (storedData == null) {
            LogUtil.warn(getClassName(), "Unable to store token data to form. Encountered invalid form ID of '" + formDefId + "'.");
        }
    }
    
    private void storeNftDataToForm(FormRow row, String tokenId, TransactionRecord transactionRecord) {
        String formDefId = getPropertyString("formDefIdStoreNftData");
        
        if (formDefId == null || formDefId.isEmpty()) {
            LogUtil.warn(getClassName(), "Unable to store new NFT data to form. Encountered blank form ID.");
            return;
        }
        
        String minterAccountId = row.getProperty(getPropertyString("minterAccountId"));
        
        final boolean isTest = client.getLedgerId().isTestnet() || client.getLedgerId().isPreviewnet();
        
        String nftSerialNumberField = getPropertyString("nftSerialNumberField");
        String nftAssociatedTokenIdField = getPropertyString("nftAssociatedTokenIdField");
        String minterAccountField = getPropertyString("nftMinterAccountField");
        String isNftOnTestnetField = getPropertyString("isNftOnTestnetField");

        FormRow newRow = new FormRow();

        newRow = addRow(newRow, nftSerialNumberField, transactionRecord.receipt.serials.get(0).toString());
        newRow = addRow(newRow, nftAssociatedTokenIdField, tokenId);
        newRow = addRow(newRow, minterAccountField, minterAccountId);
        newRow = addRow(newRow, isNftOnTestnetField, String.valueOf(isTest));
        
        FormRowSet storedData = storeFormRow(formDefId, newRow);
        if (storedData == null) {
            LogUtil.warn(getClassName(), "Unable to store NFT data to form. Encountered invalid form ID of '" + formDefId + "'.");
        }
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        
        return row;
    }
}
