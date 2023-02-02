package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import org.joget.apps.form.service.FormUtil;

public class TokenUtil {
    
    private TokenUtil() {}
    
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
