package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import org.joget.commons.util.LogUtil;

public class AccountUtil {
    
    public static PrivateKey derivePrivateKeyFromMnemonic(Mnemonic mnemonic) {
        try {
            return mnemonic.toPrivateKey();
        } catch (BadMnemonicException ex) {
            LogUtil.error(PluginUtil.class.getName(), ex, "Unable to derive key from mnemonic phrase. Reason: " + ex.reason.toString());
            return null;
        }
    }
    
    public static PublicKey derivePublicKeyFromMnemonic(Mnemonic mnemonic) {
        return derivePrivateKeyFromMnemonic(mnemonic).getPublicKey();
    }
}
