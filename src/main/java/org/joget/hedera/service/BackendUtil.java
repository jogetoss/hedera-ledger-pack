package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.util.Map;
import org.joget.commons.util.LogUtil;

public class BackendUtil {
    
    public static final String MAINNET_NAME = "mainnet";
    public static final String PREVIEWNET_NAME = "previewnet";
    public static final String TESTNET_NAME = "testnet";
    
    public static Client getHederaClient(Map properties) {
        
        final String operatorId = (String) properties.get("operatorId");
        final String operatorKey = (String) properties.get("operatorKey");
        final String networkType = getNetworkType(properties);
        
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
                case TESTNET_NAME:
                default:
                    client = Client.forTestnet();
                    break;
            }
            
            client.setOperator(operatorAccountId, operatorPrivateKey);
            
        } catch (Exception ex) {
            LogUtil.error(BackendUtil.class.getName(), ex, "Unable to initialize hedera client. Reason: " + ex.getMessage());
            return null;
        }
        
        return client;
    }
    
    public static String getNetworkType(Map properties) {
        return (String) properties.get("networkType");
    }
    
    public static boolean isTestnet(Map properties) {
        String networkType = getNetworkType(properties);
        return isTestnet(networkType);
    }
    
    public static boolean isTestnet(String networkType) {
        return !(MAINNET_NAME).equals(networkType);
    }
}
