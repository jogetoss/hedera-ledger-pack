package org.joget.hedera.model;

import com.hedera.hashgraph.sdk.Client;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;

public abstract class HederaHashVariable extends DefaultHashVariablePlugin {

    protected abstract String processHashVariable(Client client, String variableKey)
            throws TimeoutException, RuntimeException;

    @Override
    public String processHashVariable(String variableKey) {
        String result = null;
        // if variableKey contains unprocessing hash variable as username
        if (variableKey.startsWith("[") && variableKey.contains("]")) {
            return null;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        try {
            final Client client = BackendUtil.getHederaClient(getProperties());

            if (client != null) {
                result = processHashVariable(client, variableKey);
            }
        } catch (TimeoutException ex) {
            LogUtil.error(getClassName(), ex, "Error executing hash variable plugin due to timeout.");
        } catch (RuntimeException ex) { // Compatibility workaround for MultiTenantPluginManager - avoid using SDK's
                                        // custom exceptions
            final String exceptionMessage = ex.getMessage();

            if (exceptionMessage.contains("PrecheckStatusException")) {
                LogUtil.error(getClassName(), ex,
                        "Error executing hash variable plugin due to failed transaction prechecks.");
            } else {
                LogUtil.error(getClassName(), ex, "Unhandled RuntimeException occured...");
            }
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error executing hash variable plugin...");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return result;
    }

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