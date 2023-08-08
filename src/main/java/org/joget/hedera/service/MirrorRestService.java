package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.LedgerId;
import java.io.IOException;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.api.ApiEndpoint;
import org.joget.hedera.model.api.rest.Arkhia;
import org.joget.hedera.model.api.rest.PublicHedera;
import org.json.JSONObject;

//Hackish. Will be improved in near future.
public class MirrorRestService {
    
    public static final String DEFAULT_REST_SERVICE = "publicHedera";
    
    private final String endpointUrl;
    private final Map properties;
    
    public MirrorRestService(Map properties, LedgerId ledgerId) {
        this.properties = BackendUtil.getBackendDefaultConfig(properties);
        this.endpointUrl = createRest((String) properties.get("backendService")).getEndpoint(ledgerId);
    }
    
    private ApiEndpoint createRest(String backendService) {
        switch (backendService) {
            case "":
            case "publicHedera":
                return new PublicHedera();
            case "arkhia" :
                return new Arkhia();
            default:
                LogUtil.warn(getClassName(), "Unknown rest service type found!");
                return null;
        }
    }
    
    public JSONObject get(String url) {
        HttpGet getRequest = new HttpGet(endpointUrl + url);
        if ("arkhia".equalsIgnoreCase((String) properties.get("backendService"))) {
            getRequest.setHeader("x-api-key", (String) properties.get("arkhiaApiKey"));
        }
        return execute(getRequest);
    }
    
    private JSONObject execute(HttpRequestBase request) {
        CloseableHttpClient httpClient = null;      
        try {
            httpClient = HttpClients.createDefault();
            
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
            LogUtil.error(getClassName(), ex, "Error executing HTTP request...");
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
    
    private static String getClassName() {
        return MirrorRestService.class.getName();
    }
}
