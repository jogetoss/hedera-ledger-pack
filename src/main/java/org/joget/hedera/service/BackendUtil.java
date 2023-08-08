package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.hedera.lib.plugindefaultproperties.HederaDefaultBackendConfigurator;
import org.joget.plugin.property.service.PropertyUtil;

public class BackendUtil {
    
    private BackendUtil() {}
    
    public static Client getHederaClient(Map properties) {
        properties = getBackendDefaultConfig(properties);
        
        final AccountId operatorAccountId = AccountId.fromString((String) properties.get("operatorId"));
        final PrivateKey operatorPrivateKey = PrivateKey.fromString((String) properties.get("operatorKey"));
        
        if ("arkhia".equalsIgnoreCase((String) properties.get("backendService"))) {
            final String networkType = (String) properties.get("arkhiaNetworkType");
            
            try {
                return Client
                        .forName(networkType)
                        .setOperator(operatorAccountId, operatorPrivateKey)
                        .setMirrorNetwork(List.of("grpc." + networkType + ".arkhia.io:443"))
                        .setMaxAttempts(20)
                        .setMaxBackoff(Duration.ofSeconds(2L));
            } catch (Exception ex) {
                LogUtil.error(getClassName(), ex, "Unable to initialize backend service");
                return null;
            }
        } else {
            return Client
                    .forName((String) properties.get("networkType"))
                    .setOperator(operatorAccountId, operatorPrivateKey)
                    .setMaxAttempts(20)
                    .setMaxBackoff(Duration.ofSeconds(2L));
        }
    }
    
    public static Map getBackendDefaultConfig(Map properties) {
        Map defaultProps = getBackendDefaultConfig();
        if (defaultProps != null) {
            properties.putAll(defaultProps);
        }
        
        return properties;
    }
    
    public static Map getBackendDefaultConfig() {
        PluginDefaultPropertiesDao pluginDefaultPropertiesDao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
        PluginDefaultProperties prop = pluginDefaultPropertiesDao.loadById(
                HederaDefaultBackendConfigurator.class.getCanonicalName(), 
                AppUtil.getCurrentAppDefinition()
        );
        
        if (prop == null) {
            return null;
        }
        
        return PropertyUtil.getPropertiesValueFromJson(
            AppUtil.processHashVariable(prop.getPluginProperties(), null, StringUtil.TYPE_JSON, null)
        );
    }
    
    private static String getClassName() {
        return BackendUtil.class.getName();
    }
}
