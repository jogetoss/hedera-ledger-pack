package org.joget.hedera.model.explorer;

import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.NetworkType;

public class HashScan implements Explorer {

    private static final String ENDPOINT_MAINNET = "https://hashscan.io/mainnet/";
    private static final String ENDPOINT_PREVIEWNET = "https://hashscan.io/previewnet/";
    private static final String ENDPOINT_TESTNET = "https://hashscan.io/testnet/";
    
    private final String endpointUrl;
    
    public HashScan(NetworkType networkType) {        
        switch (networkType) {
            case MAINNET:
                this.endpointUrl = ENDPOINT_MAINNET;
                break;
            case PREVIEWNET:
                this.endpointUrl = ENDPOINT_PREVIEWNET;
                break;
            case TESTNET:
                this.endpointUrl = ENDPOINT_TESTNET;
                break;
            default:
                LogUtil.warn(getClassName(), "Unknown network type found!");
                this.endpointUrl = "";
        }
    }
    
    @Override
    public String getTransactionUrl(String transactionId) {
        //No need to return immediately, in case user wants to show link as is
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = "";
        }
        
        return endpointUrl + "transaction/" + transactionId.replaceAll("[^0-9]","");
    }

    @Override
    public String getAccountUrl(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            accountId = "";
        }
        
        return endpointUrl + "account/" + accountId;
    }

    @Override
    public String getTokenUrl(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            tokenId = "";
        }
        
        return endpointUrl + "token/" + tokenId;
    }

    @Override
    public String getTopicUrl(String topicId) {
        if (topicId == null || topicId.isBlank()) {
            topicId = "";
        }
        
        return endpointUrl + "topic/" + topicId;
    }
    
    private static String getClassName() {
        return HashScan.class.getName();
    }
}
