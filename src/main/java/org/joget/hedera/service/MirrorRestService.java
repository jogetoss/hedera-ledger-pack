package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.LedgerId;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
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
        return switch (backendService) {
            case "", "publicHedera" -> new PublicHedera();
            case "arkhia" -> new Arkhia();
            default -> {
                LogUtil.warn(getClassName(), "Unknown rest service type found!");
                yield null;
            }
        };
    }
    
    public JSONObject getAccountData(String accountId) {
        return get("accounts/" + accountId);
    }
    
    public JSONObject getTokenData(String tokenId) {
        return get("tokens/" + tokenId);
    }
    
    public JSONObject getNftData(String tokenId, String nftSerialNumber) {
        return get("tokens/" + tokenId + "/nfts/" + nftSerialNumber);
    }
    
    public JSONObject getTxData(String txId) {
        return get("transactions/" + txId);
    }
    
    public JSONObject getScheduleData(String scheduleId) {
        return get("schedules/" + scheduleId);
    }
    
    public JSONObject getAllTopicMessages(String topicId) {
        return get("topics/" + topicId + "/messages");
    }
    
    public JSONObject getTopicMessage(String topicId, String sequenceNumber) {
        return get("topics/" + topicId + "/messages/" + sequenceNumber);
    }
    
    public JSONObject get(String url) {
        HttpGet getRequest = new HttpGet(endpointUrl + url);
        if ("arkhia".equalsIgnoreCase((String) properties.get("backendService"))) {
            getRequest.setHeader("x-api-key", (String) properties.get("arkhiaApiKey"));
        }
        return execute(getRequest);
    }
    
    private JSONObject execute(HttpUriRequestBase request) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String jsonResponse = httpClient.execute(request, response -> EntityUtils.toString(response.getEntity(), "UTF-8"));
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                return null;
            }

            if (jsonResponse.startsWith("[") && jsonResponse.endsWith("]")) {
                jsonResponse = "{ \"response\" : " + jsonResponse + " }";
            }

            return new JSONObject(jsonResponse);
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error executing HTTP request...");
        }
        
        return null;
    }
    
    private static String getClassName() {
        return MirrorRestService.class.getName();
    }
}
