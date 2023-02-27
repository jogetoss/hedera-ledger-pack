package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import java.util.Map;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.NetworkType;
import org.json.JSONObject;

public class TokenUtil {
    
    private TokenUtil() {}
    
    public static boolean isTokenExist(Map properties, String tokenId) {
        //If within a Form Builder, don't make useless API calls
        if (tokenId == null || tokenId.isBlank() || FormUtil.isFormBuilderActive()) {
            return false;
        }
        
        final NetworkType networkType = BackendUtil.getNetworkType(properties);
        
        String getUrl = networkType.getMirrorNodeUrl()
                + "tokens/" 
                + tokenId;
        
        JSONObject jsonResponse = BackendUtil.httpGet(getUrl);
        
        try {
            return (jsonResponse != null && (jsonResponse.getString("token_id")).equals(tokenId));
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Abnormal API response detected...");
        }
        
        return false;
    }
    
    public static boolean isTokenExist(Client client, String tokenId) {
        //If within a Form Builder, don't make useless API calls
        if (tokenId == null || tokenId.isBlank() || FormUtil.isFormBuilderActive()) {
            return false;
        }
        
        try {
            TokenInfo tokenInfo = new TokenInfoQuery()
                    .setTokenId(TokenId.fromString(tokenId))
                    .execute(client);
            
            return (tokenInfo != null);
        } catch (Exception ex) {
            //Ignore if not successful.
        }
        
        return false;
    }
    
    private static String getClassName() {
        return TokenUtil.class.getName();
    }
}
