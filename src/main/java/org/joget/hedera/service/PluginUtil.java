package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import java.io.IOException;
import java.util.Properties;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;

public class PluginUtil {
    
    public static final String MULTI_VALUE_DELIMITER = ";";
    
    public static final String MESSAGE_PATH = "messages/HederaMessages";
    
    public static final String FORM_ELEMENT_CATEGORY = "Hedera";
    
    public static String getProjectVersion(Class classObj) {
        final Properties projectProp = new Properties();
        try {
            projectProp.load(classObj.getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException ex) {
            LogUtil.error(classObj.getName(), ex, "Unable to get project version from project properties...");
        }
        return projectProp.getProperty("version");
    }
    
    //Feel free to implement more secure encryption algo
    public static String encrypt(String content) {
        return SecurityUtil.encrypt(content);
    }
    
    //Feel free to implement more secure encryption algo, and decrypt accordingly
    public static String decrypt(String content) {
        return SecurityUtil.decrypt(content);
    }
    
    public static PrivateKey derivePrivateKeyFromMnemonic(Mnemonic mnemonic) {
        try {
            return mnemonic.toPrivateKey();
        } catch (BadMnemonicException ex) {
            LogUtil.error(PluginUtil.class.getName(), ex, "Unable to derive key from mnemonic phrase. Reason: " + ex.reason.toString());
            return null;
        }
    }
    
    public static PublicKey derivePublicKeyFromMnemonic(Mnemonic mnemonic) {
        try {
            return mnemonic.toPrivateKey().getPublicKey();
        } catch (BadMnemonicException ex) {
            LogUtil.error(PluginUtil.class.getName(), ex, "Unable to derive key from mnemonic phrase. Reason: " + ex.reason.toString());
            return null;
        }
    }
}
