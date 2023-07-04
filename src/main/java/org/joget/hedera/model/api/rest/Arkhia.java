package org.joget.hedera.model.api.rest;

import com.hedera.hashgraph.sdk.LedgerId;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.api.ApiEndpoint;

public class Arkhia implements ApiEndpoint {

    private static final String POSTFIX = "api/v1/";
    
    private static final String MAINNET_URL = "https://pool.arkhia.io/hedera/mainnet/" + POSTFIX;
    private static final String PREVIEWNET_URL = ""; //Not available
    private static final String TESTNET_URL = "https://pool.arkhia.io/hedera/testnet/" + POSTFIX;
    
    @Override
    public String getEndpoint(LedgerId ledgerId) {
        if (ledgerId.isMainnet()) {
            return MAINNET_URL;
        } else if (ledgerId.isPreviewnet()) {
            LogUtil.warn(this.getClass().getName(), "Previewnet endpoint not available...");
            return PREVIEWNET_URL;
        } else if (ledgerId.isTestnet()) {
            return TESTNET_URL;
        } else {
            LogUtil.warn(this.getClass().getName(), "Unknown client network found!");
            return "";
        }
    }
}
