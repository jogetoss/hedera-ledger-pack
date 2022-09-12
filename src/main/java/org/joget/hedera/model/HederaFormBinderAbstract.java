package org.joget.hedera.model;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
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
     * Loads data based on a primary key. This method is wrapped by load().
     * @param client The Hedera client to execute queries and actions
     * @param element The element to load the data into.
     * @param primaryKey
     * @param formData
     * @return A Collection of Map objects. Each Map object contains property=value pairs to represent a data row.
     */
    public abstract FormRowSet loadData(Client client, Element element, String primaryKey, FormData formData)
            throws TimeoutException, PrecheckStatusException;
    
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        FormRowSet rows = new FormRowSet();
        
        try {
            final Client client = BackendUtil.getHederaClient(getProperties());
            
            if (client != null) {
                rows = loadData(client, element, primaryKey, formData);
            }
        } catch (TimeoutException ex) {
            LogUtil.error(getClassName(), ex, "Error executing form binder plugin due to timeout.");
        } catch (PrecheckStatusException ex) {
            LogUtil.error(getClassName(), ex, "Error executing form binder plugin due to failed transaction prechecks.");
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
