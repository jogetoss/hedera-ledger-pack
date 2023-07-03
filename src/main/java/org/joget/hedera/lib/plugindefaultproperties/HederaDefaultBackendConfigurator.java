package org.joget.hedera.lib.plugindefaultproperties;

import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.plugin.base.DefaultAuditTrailPlugin;

/**
 * Used to store backend configs to reuse throughout an entire app
*/
public class HederaDefaultBackendConfigurator extends DefaultAuditTrailPlugin {
    
    @Override
    public String getName() {
        return "Hedera Default Backend Configurator";
    }

    @Override
    public String getDescription() {
        return "Store backend configurations for the Hedera Ledger Pack, to be applied throughout an entire app.";
    }
    
    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.forceReadGenericBackendConfigs(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/plugindefaultproperties/HederaDefaultBackendConfigurator.json", new String[]{backendConfigs}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public Object execute(Map props) { return null; } //Do nothing
    
    @Override
    public String getVersion() {
        return PluginUtil.getProjectVersion(this.getClass());
    }
    
    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }
}
