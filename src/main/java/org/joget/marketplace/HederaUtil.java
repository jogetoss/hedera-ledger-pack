package org.joget.marketplace;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

public class HederaUtil {
    
    public static final String MULTI_VALUE_DELIMITER = ";";
    
    public static final String MAINNET_NAME = "mainnet";
    public static final String PREVIEWNET_NAME = "previewnet";
    public static final String TESTNET_NAME = "testnet";
    
    //Using DragonGlass Hedera explorer
    public static final String TESTNET_TX_EXPLORER_URL = "https://testnet.dragonglass.me/hedera/transactions/";
    public static final String PREVIEWNET_TX_EXPLORER_URL = "";
    public static final String MAINNET_TX_EXPLORER_URL = "https://app.dragonglass.me/hedera/transactions/";
    
    public static Client getHederaClient(String operatorId, String operatorKey, String networkType) {
        
        final AccountId operatorAccountId = AccountId.fromString(operatorId);
        final PrivateKey operatorPrivateKey = PrivateKey.fromString(operatorKey);
        
        Client client = null;
        
        try {
            //Always default to "testnet" in case of encountering odd networkType value
            switch (networkType) {
                case MAINNET_NAME:
                    client = Client.forMainnet();
                    break;
                case PREVIEWNET_NAME:
                    client = Client.forPreviewnet();
                    break;
                default:
                    client = Client.forTestnet();
                    break;
            }
            
            client.setOperator(operatorAccountId, operatorPrivateKey);
            
        } catch (Exception ex) {
            LogUtil.error(HederaUtil.class.getName(), ex, "Unable to initialize hedera client. Reason: " + ex.getMessage());
            return null;
        }
        
        return client;
    }
    
    public static String getTransactionExplorerUrl(String networkType, String transactionId) {
        String transactionUrl = "";
        
        switch (networkType) {
            case MAINNET_NAME:
                transactionUrl = MAINNET_TX_EXPLORER_URL;
                break;
            case PREVIEWNET_NAME:
                // At the time of developing this, no handy previewnet explorers available yet
                break;
            default:
                transactionUrl = TESTNET_TX_EXPLORER_URL;
                break;
        }
        
        if (transactionUrl.isEmpty()) {
            return null;
        }
        
        //Remove any characters from string except numbers, to adapt to dragonglass url format
        transactionUrl += transactionId.replaceAll("[^0-9]","");
        
        return transactionUrl;
    }
    
    public static String convertInstantToZonedDateTimeString(Instant timeStamp) {
        if (timeStamp == null) {
            return "";
        }
        
        return convertInstantToZonedDateTimeString(timeStamp, null);
    }
    
    public static String convertInstantToZonedDateTimeString(Instant timeStamp, String zoneId) {
        String dateTimeFormat = "yyyy-MM-dd hh:mm:ss a z";
        
        //Default to UTC timezone
        ZoneId zone;
        if (zoneId != null && !zoneId.isEmpty()) {
            zone = ZoneId.of(zoneId);
        } else {
            zone = ZoneId.of("UTC");
        }
        
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(timeStamp, zone);
        
        return DateTimeFormatter.ofPattern(dateTimeFormat).format(dateTime);
    }
    
    public static PrivateKey derivePrivateKeyFromMnemonic(Mnemonic mnemonic) {
        try {
            return mnemonic.toPrivateKey();
        } catch (BadMnemonicException ex) {
            LogUtil.error(HederaUtil.class.getName(), ex, "Unable to derive key from mnemonic phrase. Reason: " + ex.reason.toString());
            return null;
        }
    }
    
    public static PublicKey derivePublicKeyFromMnemonic(Mnemonic mnemonic) {
        try {
            return mnemonic.toPrivateKey().getPublicKey();
        } catch (BadMnemonicException ex) {
            LogUtil.error(HederaUtil.class.getName(), ex, "Unable to derive key from mnemonic phrase. Reason: " + ex.reason.toString());
            return null;
        }
    }
    
    //Feel free to implement more secure encryption algo
    public static String encrypt(String content) {
        content = SecurityUtil.encrypt(content);
        
        return content;
    }
    
    //Feel free to implement more secure encryption algo, and decrypt accordingly
    public static String decrypt(String content) {
        content = SecurityUtil.decrypt(content);
        
        return content;
    }
}
