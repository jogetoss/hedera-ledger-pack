package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import org.joget.commons.util.LogUtil;

public class BackendUtil {
    
    public static final String MAINNET_NAME = "mainnet";
    public static final String PREVIEWNET_NAME = "previewnet";
    public static final String TESTNET_NAME = "testnet";
    
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
            LogUtil.error(BackendUtil.class.getName(), ex, "Unable to initialize hedera client. Reason: " + ex.getMessage());
            return null;
        }
        
        return client;
    }
}
