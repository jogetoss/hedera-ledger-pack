package org.joget.hedera.model.api.rest;

import com.hedera.hashgraph.sdk.LedgerId;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.api.ApiEndpoint;

public class PublicHedera implements ApiEndpoint {

    private static final String POSTFIX = "api/v1/";
    
    private static final String MAINNET_URL = "https://mainnet-public.mirrornode.hedera.com/" + POSTFIX;
    private static final String PREVIEWNET_URL = "https://previewnet.mirrornode.hedera.com/" + POSTFIX;
    private static final String TESTNET_URL = "https://testnet.mirrornode.hedera.com/" + POSTFIX;
    
    @Override
    public String getEndpoint(LedgerId ledgerId) {
        if (ledgerId.isMainnet()) {
            return MAINNET_URL;
        } else if (ledgerId.isPreviewnet()) {
            return PREVIEWNET_URL;
        } else if (ledgerId.isTestnet()) {
            return TESTNET_URL;
        } else {
            LogUtil.warn(this.getClass().getName(), "Unknown client network found!");
            return "";
        }
    }
}
