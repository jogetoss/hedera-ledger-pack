package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.NetworkType;
import org.json.JSONObject;

public class AccountUtil {
    
    private AccountUtil() {}
    
    public static boolean isAccountExist(NetworkType networkType, String accountId) {
        //If within a Form Builder, don't make useless API calls
        if (accountId == null || accountId.isBlank() || FormUtil.isFormBuilderActive()) {
            return false;
        }
        
        String getUrl = networkType.getMirrorNodeUrl()
                + "accounts/" 
                + accountId;
        
        JSONObject jsonResponse = BackendUtil.httpGet(getUrl);
        
        try {
            return (jsonResponse != null && (jsonResponse.getString("account")).equals(accountId));
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Abnormal API response detected...");
        }
        
        return false;
    }
    
    public static PrivateKey derivePrivateKeyFromMnemonic(Mnemonic mnemonic) {
        try {
            return mnemonic.toStandardEd25519PrivateKey("", 0);
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Unable to derive key from mnemonic phrase...");
            return null;
        }
    }
    
    public static AccountId getAccountIdFromMnemonic(Mnemonic mnemonic) {
        return derivePrivateKeyFromMnemonic(mnemonic).toAccountId(0, 0);
    }
    
    public static PublicKey derivePublicKeyFromMnemonic(Mnemonic mnemonic) {
        return derivePrivateKeyFromMnemonic(mnemonic).getPublicKey();
    }
    
    private static String getClassName() {
        return AccountUtil.class.getName();
    }
}
