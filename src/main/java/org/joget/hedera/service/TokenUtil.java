package org.joget.hedera.service;

import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.NetworkType;
import org.json.JSONObject;

public class TokenUtil {
    
    private TokenUtil() {}
    
    public static boolean isTokenExist(NetworkType networkType, String tokenId) {
        //If within a Form Builder, don't make useless API calls
        if (tokenId == null || tokenId.isBlank() || FormUtil.isFormBuilderActive()) {
            return false;
        }
        
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
    
    private static String getClassName() {
        return TokenUtil.class.getName();
    }
}
