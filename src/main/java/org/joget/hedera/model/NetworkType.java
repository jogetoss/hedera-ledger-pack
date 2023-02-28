package org.joget.hedera.model;

import com.hedera.hashgraph.sdk.Client;
import org.joget.commons.util.LogUtil;

/**
 * Carries info about Hedera network, such as string values used in plugin, client types, and mirror node API endpoints
 */
public enum NetworkType {
    
    MAINNET("mainnet", Client.forMainnet(), "https://mainnet-public.mirrornode.hedera.com/"),
    PREVIEWNET("previewnet", Client.forPreviewnet(), "https://previewnet.mirrornode.hedera.com/"),
    TESTNET("testnet", Client.forTestnet(), "https://testnet.mirrornode.hedera.com/");
    
    private final String value;
    private final Client client;
    private final String mirrorNodeUrl;
    
    NetworkType(final String value, final Client client, final String mirrorNodeUrl) {
        this.value = value;
        this.client = client;
        this.mirrorNodeUrl = mirrorNodeUrl + "api/v1/";
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    public Client getClient() {
        return this.client;
    }
    
    public String getMirrorNodeUrl() {
        return this.mirrorNodeUrl;
    }
    
    public static NetworkType fromString(String text) {
        for (NetworkType type : NetworkType.values()) {
            if ((type.value).equalsIgnoreCase(text)) {
                return type;
            }
        }
        
        LogUtil.warn(getClassName(), "Unknown network type found!");
        return null;
    }
    
    public static NetworkType fromClient(Client client) {
        if (client != null) {
            for (NetworkType type : NetworkType.values()) {
                if ((type.client.getLedgerId()).equals(client.getLedgerId())) {
                    return type;
                }
            }
        }
        
        LogUtil.warn(getClassName(), "Unknown client type found!");
        return null;
    }
    
    private static String getClassName() {
        return NetworkType.class.getName();
    }
}
