package org.joget.hedera.service;

import java.util.Map;

public class ExplorerUtil {
    
    private static final String DRAGONGLASS_TYPE = "dragonglass";
    
    //Hedera explorer links
    public static final String DRAGONGLASS_MAINNET = "https://app.dragonglass.me/hedera/";
    public static final String DRAGONGLASS_TESTNET = "https://testnet.dragonglass.me/hedera/";
    public static final String DRAGONGLASS_PREVIEWNET = ""; //Not available

    
    public static String getTransactionExplorerUrl(Map properties, String transactionId) {
        return getTransactionExplorerUrl(properties, transactionId, "");
    }
    
    public static String getTransactionExplorerUrl(Map properties, String transactionId, String explorerType) {
        //No need to return immediately, in case user wants to show link as is
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = "";
        }

        String explorerUrl;
        
        switch (explorerType) {
            case DRAGONGLASS_TYPE:
            default:
                explorerUrl = BackendUtil.isTestnet(properties) ? DRAGONGLASS_TESTNET : DRAGONGLASS_MAINNET;
                explorerUrl += "transactions/";
                break;
        }
        
        explorerUrl += transactionId.replaceAll("[^0-9]","");
        
        return explorerUrl;
    }
}
