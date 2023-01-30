package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.TransactionRecord;
import java.time.Instant;
import java.util.Map;
import org.joget.commons.util.LogUtil;
import static org.joget.hedera.service.BackendUtil.MAINNET_NAME;
import static org.joget.hedera.service.BackendUtil.PREVIEWNET_NAME;
import static org.joget.hedera.service.BackendUtil.TESTNET_NAME;

public class ExplorerUtil {
    
    private static final String HASHSCAN_TYPE = "hashscan";
    private static final String DRAGONGLASS_TYPE = "dragonglass";
    
    //Hedera explorer links
    private static final String HASHSCAN_MAINNET = "https://hashscan.io/mainnet/";
    private static final String HASHSCAN_TESTNET = "https://hashscan.io/testnet/";
    private static final String HASHSCAN_PREVIEWNET = "https://hashscan.io/previewnet/";
    
    private static final String DRAGONGLASS_MAINNET = "https://app.dragonglass.me/";
    private static final String DRAGONGLASS_TESTNET = "https://testnet.dragonglass.me/";
    private static final String DRAGONGLASS_PREVIEWNET = ""; //Not available

    private static String getHashscanUrl(String networkType) {
        switch (networkType) {
            case MAINNET_NAME:
                return HASHSCAN_MAINNET;
            case TESTNET_NAME:
                return HASHSCAN_TESTNET;
            case PREVIEWNET_NAME:
                return HASHSCAN_PREVIEWNET;
            default:
                LogUtil.warn(ExplorerUtil.class.getName(), "Unknown network selection found!");
                return null;
        }
    }
    
    private static String getDragonglassUrl(String networkType) {
        switch (networkType) {
            case MAINNET_NAME:
                return DRAGONGLASS_MAINNET;
            case TESTNET_NAME:
                return DRAGONGLASS_TESTNET;
            case PREVIEWNET_NAME:
                return DRAGONGLASS_PREVIEWNET;
            default:
                LogUtil.warn(ExplorerUtil.class.getName(), "Unknown network selection found!");
                return null;
        }
    }
    
    //Default is Hashscan for all transaction URLs
    public static String getTransactionExplorerUrl(Map properties, TransactionRecord txRecord) {
        return getTransactionExplorerUrl(properties, txRecord, HASHSCAN_TYPE);
    }
    
    public static String getTransactionExplorerUrl(Map properties, TransactionRecord txRecord, String explorerType) {
        String transactionId = txRecord.transactionId.toString();
        
        //No need to return immediately, in case user wants to show link as is
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = "";
        }

        String networkType = BackendUtil.getNetworkType(properties);
        
        switch (explorerType) {
            case DRAGONGLASS_TYPE: {
                String formattedTxId = transactionId.replaceAll("[^0-9]","");
                return getDragonglassUrl(networkType) + "transactions/" + formattedTxId;
            }
            case HASHSCAN_TYPE:
            default:
                Instant consensusTimestamp = txRecord.consensusTimestamp;
                String formattedTimestamp = String.valueOf(consensusTimestamp.getEpochSecond()) + "." + String.valueOf(consensusTimestamp.getNano());
                String formattedTxId = transactionId.replaceAll("@", "-");
                formattedTxId = formattedTxId.substring(0, formattedTxId.lastIndexOf(".")) + "-" + formattedTxId.substring(formattedTxId.lastIndexOf(".") + 1);
                return getHashscanUrl(networkType) + "transaction/" + formattedTimestamp + "?tid=" + formattedTxId;
        }
    }
}
