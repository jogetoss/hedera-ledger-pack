package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.io.IOException;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class BackendUtil {
    
    private BackendUtil() {}
    
    public static final String MAINNET_NAME = "mainnet";
    public static final String PREVIEWNET_NAME = "previewnet";
    public static final String TESTNET_NAME = "testnet";
    
    public static Client getHederaClient(Map properties) {
        
        final String operatorId = WorkflowUtil.processVariable((String) properties.get("operatorId"), "", null);
        final String operatorKey = WorkflowUtil.processVariable((String) properties.get("operatorKey"), "", null);
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
            LogUtil.error(getClassName(), ex, "Unable to initialize hedera client. Reason: " + ex.getMessage());
            return null;
        }
        
        return client;
    }
    
    public static String getMirrorNodeUrl(String networkType) {
        switch (networkType) {
            case MAINNET_NAME:
                return "https://mainnet-public.mirrornode.hedera.com/api/v1/";
            case TESTNET_NAME:
                return "https://testnet.mirrornode.hedera.com/api/v1/";
            case PREVIEWNET_NAME:
                return "https://previewnet.mirrornode.hedera.com/api/v1/";
            default:
                LogUtil.warn(getClassName(), "Unknown network selection found!");
                return null;
        }
    }
    
    public static JSONObject httpGet(String url) {
        HttpGet request = null;
        CloseableHttpClient httpClient = null;
        
        try {
            httpClient = HttpClients.createDefault();
            
            request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                return null;
            }
            
            if (jsonResponse.startsWith("[") && jsonResponse.endsWith("]")) {
                jsonResponse = "{ \"response\" : " + jsonResponse + " }";
            }
            
            return new JSONObject(jsonResponse);
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error performing HTTP GET call...");
        } finally {
            try {
                if (request != null) {
                    request.releaseConnection();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException ex) {
                LogUtil.error(getClassName(), ex, "");
            }
        }
        
        return null;
    }
    
    public static String getNetworkType(Map properties) {
        return (String) properties.get("networkType");
    }
    
    public static boolean isTestnet(Map properties) {
        return !(MAINNET_NAME).equals(getNetworkType(properties));
    }
    
    private static String getClassName() {
        return BackendUtil.class.getName();
    }
}
