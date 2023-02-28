package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.TransactionRecord;
import java.time.Instant;
import java.util.Map;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.NetworkType;
import org.json.JSONObject;

public class ExplorerUtil {
    
    private ExplorerUtil() {}
    
    public static String getTransactionUrl(Map properties, String transactionId, String explorerType) {
        final NetworkType networkType = BackendUtil.getNetworkType(properties);
        final ExplorerType explorer = ExplorerType.fromString(explorerType);
        
        final String url = ExplorerEndpoint.getUrl(explorer, networkType);
        
        switch (explorer) {
            case DRAGONGLASS: {
                String formattedTxId = transactionId.replaceAll("[^0-9]","");
                return url 
                        + "transactions/" 
                        + formattedTxId;
            }
            case HASHSCAN:
            default:
                String formattedTxId = transactionId.replaceAll("@", "-");
                formattedTxId = formattedTxId.substring(0, formattedTxId.lastIndexOf(".")) 
                        + "-" 
                        + formattedTxId.substring(formattedTxId.lastIndexOf(".") + 1);

                try {
                    String getUrl = networkType.getMirrorNodeUrl()
                            + "transactions/"
                            + formattedTxId;
                    JSONObject jsonResponse = BackendUtil.httpGet(getUrl);

                    String consensusTimestamp = jsonResponse.getJSONArray("transactions").getJSONObject(0).getString("consensus_timestamp");

                    return url
                            + "transaction/"
                            + consensusTimestamp
                            + "?tid="
                            + formattedTxId;
                } catch (Exception ex) {
                    LogUtil.error(getClassName(), ex, "Abnormal API response detected...");
                }
        }
        
        return null;
    }
    
    //Default is Hashscan for all transaction URLs
    public static String getTransactionUrl(Map properties, TransactionRecord txRecord) {
        return getTransactionUrl(properties, txRecord, ExplorerType.HASHSCAN.value);
    }
    
    public static String getTransactionUrl(Map properties, TransactionRecord txRecord, String explorerType) {
        String transactionId = txRecord.transactionId.toString();
        
        //No need to return immediately, in case user wants to show link as is
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = "";
        }

        final NetworkType networkType = BackendUtil.getNetworkType(properties);
        final ExplorerType explorer = ExplorerType.fromString(explorerType);
        
        final String url = ExplorerEndpoint.getUrl(explorer, networkType);
        
        switch (explorer) {
            case DRAGONGLASS: {
                String formattedTxId = transactionId.replaceAll("[^0-9]","");
                return url
                        + "transactions/"
                        + formattedTxId;
            }
            case HASHSCAN:
            default:
                Instant consensusTimestamp = txRecord.consensusTimestamp;
                String formattedTimestamp = String.valueOf(consensusTimestamp.getEpochSecond()) + "." + String.valueOf(consensusTimestamp.getNano());
                String formattedTxId = transactionId.replaceAll("@", "-");
                formattedTxId = formattedTxId.substring(0, formattedTxId.lastIndexOf(".")) + "-" + formattedTxId.substring(formattedTxId.lastIndexOf(".") + 1);
                return url
                        + "transaction/"
                        + formattedTimestamp
                        + "?tid="
                        + formattedTxId;
        }
    }
    
    public static String getAddressUrl(Map properties, String accountAddress, String explorerType) {
        //No need to return immediately, in case user wants to show link as is
        if (accountAddress == null || accountAddress.isBlank()) {
            accountAddress = "";
        }
        
        final NetworkType networkType = BackendUtil.getNetworkType(properties);
        final ExplorerType explorer = ExplorerType.fromString(explorerType);
        
        final String url = ExplorerEndpoint.getUrl(explorer, networkType);
        
        switch (explorer) {
            case DRAGONGLASS:
                return url
                        + "accounts/"
                        + accountAddress;
            case HASHSCAN:
            default:
                return url
                        + "account/"
                        + accountAddress;
        }
    }
    
    public static String getTokenUrl(Map properties, String tokenId, String explorerType) {
        //No need to return immediately, in case user wants to show link as is
        if (tokenId == null || tokenId.isBlank()) {
            tokenId = "";
        }
        
        final NetworkType networkType = BackendUtil.getNetworkType(properties);
        final ExplorerType explorer = ExplorerType.fromString(explorerType);
        
        final String url = ExplorerEndpoint.getUrl(explorer, networkType);
        
        switch (explorer) {
            case DRAGONGLASS:
                return url
                        + "tokens/"
                        + tokenId;
            case HASHSCAN:
            default:
                return url
                        + "token/"
                        + tokenId;
        }
    }
    
    private static String getClassName() {
        return ExplorerUtil.class.getName();
    }
    
    private enum ExplorerEndpoint {
        
        DRAGONGLASS_MAINNET(ExplorerType.DRAGONGLASS, NetworkType.MAINNET, "https://app.dragonglass.me/"),
        DRAGONGLASS_PREVIEWNET(ExplorerType.DRAGONGLASS, NetworkType.PREVIEWNET, ""), //Not available
        DRAGONGLASS_TESTNET(ExplorerType.DRAGONGLASS, NetworkType.TESTNET, "https://testnet.dragonglass.me/"),
        
        HASHSCAN_MAINNET(ExplorerType.HASHSCAN, NetworkType.MAINNET, "https://hashscan.io/mainnet/"),
        HASHSCAN_PREVIEWNET(ExplorerType.HASHSCAN, NetworkType.PREVIEWNET, "https://hashscan.io/previewnet/"),
        HASHSCAN_TESTNET(ExplorerType.HASHSCAN, NetworkType.TESTNET, "https://hashscan.io/testnet/");
        
        private final ExplorerType explorerType;
        private final NetworkType networkType;
        private final String endpointUrl;
        
        ExplorerEndpoint(ExplorerType explorerType, NetworkType networkType, String endpointUrl) {
            this.explorerType = explorerType;
            this.networkType = networkType;
            this.endpointUrl = endpointUrl;
        }
        
        @Override
        public String toString() {
            return endpointUrl;
        }
        
        public static String getUrl(ExplorerType explorerType, NetworkType networkType) {
            for (ExplorerEndpoint endpoint : ExplorerEndpoint.values()) {
                if ((endpoint.explorerType).equals(explorerType) && (endpoint.networkType).equals(networkType)) {
                    return endpoint.endpointUrl;
                }
            }

            LogUtil.warn(getClassName(), "Unknown endpoint selection found!");
            return null;
        }
    }
    
    private enum ExplorerType {
            
        DRAGONGLASS("dragonglass"),
        HASHSCAN("hashscan");

        private final String value;

        ExplorerType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static ExplorerType fromString(String value) {
            for (ExplorerType type : ExplorerType.values()) {
                if ((type.value).equalsIgnoreCase(value)) {
                    return type;
                }
            }

            LogUtil.warn(getClassName(), "Unknown explorer type found!");
            return null;
        }
    }
}
