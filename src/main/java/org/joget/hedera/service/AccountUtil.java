package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import org.joget.commons.util.LogUtil;

public class AccountUtil {
    
    private AccountUtil() {}
    
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
