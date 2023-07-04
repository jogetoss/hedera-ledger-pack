package org.joget.hedera.model.explorer;

import org.joget.commons.util.LogUtil;
import com.hedera.hashgraph.sdk.LedgerId;

public class DragonGlass implements Explorer {

    private static final String ENDPOINT_MAINNET = "https://app.dragonglass.me/";
    private static final String ENDPOINT_PREVIEWNET = ""; //Not available
    private static final String ENDPOINT_TESTNET = "https://testnet.dragonglass.me/";
    
    private final String endpointUrl;

    public DragonGlass(final LedgerId ledgerId) {
        if (ledgerId.isMainnet()) {
            this.endpointUrl = ENDPOINT_MAINNET;
        } else if (ledgerId.isPreviewnet()) {
            LogUtil.warn(getClassName(), "Previewnet URL not available...");
            this.endpointUrl = ENDPOINT_PREVIEWNET;
        } else if (ledgerId.isTestnet()) {
            this.endpointUrl = ENDPOINT_TESTNET;
        } else {
            LogUtil.warn(getClassName(), "Unknown client network found!");
            this.endpointUrl = "";
        }
    }
    
    @Override
    public String getTransactionUrl(String transactionId) {
        //No need to return immediately, in case user wants to show link as is
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = "";
        }
        
        return endpointUrl + "transactions/" + transactionId.replaceAll("[^0-9]","");
    }

    @Override
    public String getAccountUrl(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            accountId = "";
        }
        
        return endpointUrl + "accounts/" + accountId;
    }

    @Override
    public String getTokenUrl(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            tokenId = "";
        }
        
        return endpointUrl + "tokens/" + tokenId;
    }

    @Override
    public String getTopicUrl(String topicId) {
        if (topicId == null || topicId.isBlank()) {
            topicId = "";
        }
        
        return endpointUrl + "topics/" + topicId;
    }
    
    private static String getClassName() {
        return DragonGlass.class.getName();
    }
}
