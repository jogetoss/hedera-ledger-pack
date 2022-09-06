package org.joget.hedera.service;

public class ExplorerUtil {
    
    //Using DragonGlass Hedera explorer
    public static final String TESTNET_TX_EXPLORER_URL = "https://testnet.dragonglass.me/hedera/transactions/";
    public static final String PREVIEWNET_TX_EXPLORER_URL = "";
    public static final String MAINNET_TX_EXPLORER_URL = "https://app.dragonglass.me/hedera/transactions/";
    
    public static String getTransactionExplorerUrl(String networkType, String transactionId) {
        String transactionUrl = "";
        
        switch (networkType) {
            case BackendUtil.MAINNET_NAME:
                transactionUrl = MAINNET_TX_EXPLORER_URL;
                break;
            case BackendUtil.PREVIEWNET_NAME:
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
}
