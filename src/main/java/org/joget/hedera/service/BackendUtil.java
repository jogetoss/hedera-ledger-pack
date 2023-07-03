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
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.hedera.lib.plugindefaultproperties.HederaDefaultBackendConfigurator;
import org.joget.hedera.model.NetworkType;
import org.joget.plugin.property.service.PropertyUtil;
import org.json.JSONObject;

public class BackendUtil {
    
    private BackendUtil() {}
    
    public static Client getHederaClient(Map properties) {
        Map defaultProps = getBackendDefaultConfig();
        if (defaultProps != null) {
            properties.putAll(defaultProps);
        }
        
        final NetworkType networkType = getNetworkType(properties);
        
        final AccountId operatorAccountId = AccountId.fromString((String) properties.get("operatorId"));
        final PrivateKey operatorPrivateKey = PrivateKey.fromString((String) properties.get("operatorKey"));
        
        return networkType.getClient().setOperator(operatorAccountId, operatorPrivateKey);
    }
    
    public static Map getBackendDefaultConfig() {
        PluginDefaultPropertiesDao pluginDefaultPropertiesDao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
        PluginDefaultProperties prop = pluginDefaultPropertiesDao.loadById(
                HederaDefaultBackendConfigurator.class.getCanonicalName(), 
                AppUtil.getCurrentAppDefinition()
        );
        
        if (prop == null) {
            return null;
        }
        
        return PropertyUtil.getPropertiesValueFromJson(
            AppUtil.processHashVariable(prop.getPluginProperties(), null, StringUtil.TYPE_JSON, null)
        );
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
    
    public static NetworkType getNetworkType(Map properties) {
        Map defaultProps = getBackendDefaultConfig();
        if (defaultProps != null) {
            properties.putAll(defaultProps);
        }
        return NetworkType.fromString((String) properties.get("networkType"));
    }
    
    public static boolean isTestnet(Map properties) {
        return !(NetworkType.MAINNET).equals(getNetworkType(properties));
    }
    
    private static String getClassName() {
        return BackendUtil.class.getName();
    }
}
