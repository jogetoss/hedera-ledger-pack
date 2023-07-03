package org.joget.hedera.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;

public class PluginUtil {
    
    private PluginUtil() {}
    
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
    
    public static String readGenericBackendConfigs(String className) {
        return BackendUtil.getBackendDefaultConfig() != null 
                ? AppUtil.readPluginResource(className, "/properties/backendConfigAlreadyExists.json") 
                : forceReadGenericBackendConfigs(className);
    }
    
    public static String forceReadGenericBackendConfigs(String className) {
        return AppUtil.readPluginResource(className, "/properties/genericBackendConfigs.json");
    }
    
    public static String readGenericWorkflowVariableMappings(String className) {
        return AppUtil.readPluginResource(className, "/properties/genericWfVarMappings.json");
    }
    
    //Feel free to implement more secure encryption algo
    public static String encrypt(String content) {
        return SecurityUtil.encrypt(content);
    }
    
    //Feel free to implement more secure encryption algo, and decrypt accordingly
    public static String decrypt(String content) {
        return SecurityUtil.decrypt(content);
    }
    
    public static String getFileHashSha256(File file) throws FileNotFoundException, IOException {        
        return DigestUtils.sha256Hex(new FileInputStream(file));
    }
    
    public static String getTextHashSha256(String text) throws FileNotFoundException, IOException {        
        return DigestUtils.sha256Hex(text);
    }
}
