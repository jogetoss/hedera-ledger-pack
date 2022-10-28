package org.joget.hedera.model;

import com.hedera.hashgraph.sdk.Client;
import java.util.concurrent.TimeoutException;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;

public abstract class HederaFormBinderAbstract extends FormBinder implements FormLoadBinder {
    
    /**
     * Used to validate necessary input values prior to executing API calls. This method is wrapped by load().
     * @return A boolean value to continue or skip plugin execution. Default value is true.
     */
    public boolean isInputDataValid() {
        return true;
    }
    
    /**
     * Loads data based on a primary key. This method is wrapped by load().
     * @param client The Hedera client to execute queries and actions
     * @param element The element to load the data into.
     * @param primaryKey
     * @param formData
     * @return A Collection of Map objects. Each Map object contains property=value pairs to represent a data row.
     */
    protected abstract FormRowSet loadData(Client client, Element element, String primaryKey, FormData formData)
            throws TimeoutException, RuntimeException;
    
    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        if (!isInputDataValid()) {
            LogUtil.debug(getClassName(), "Invalid input(s) detected. Aborting plugin execution.");
            return null;
        }
        
        FormRowSet rows = new FormRowSet();
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        try {
            final Client client = BackendUtil.getHederaClient(getProperties());
            
            if (client != null) {
                rows = loadData(client, element, primaryKey, formData);
            }
        } catch (TimeoutException ex) {
            LogUtil.error(getClassName(), ex, "Error executing form binder plugin due to timeout.");
        } catch (RuntimeException ex) { //Compatibility workaround for MultiTenantPluginManager - avoid using SDK's custom exceptions
            final String exceptionMessage = ex.getMessage();
            
            if (exceptionMessage.contains("PrecheckStatusException")) {
                LogUtil.error(getClassName(), ex, "Error executing form binder plugin due to failed transaction prechecks.");
            } else {
                LogUtil.error(getClassName(), ex, "Unhandled RuntimeException occured...");
            }
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error executing form binder plugin...");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        
        return rows;
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
