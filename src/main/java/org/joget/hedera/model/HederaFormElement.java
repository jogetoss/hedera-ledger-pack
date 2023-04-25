package org.joget.hedera.model;

import com.hedera.hashgraph.sdk.Client;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

public abstract class HederaFormElement extends Element implements FormBuilderPaletteElement {
    
    protected WorkflowAssignment wfAssignment;
    protected WorkflowManager workflowManager;
    
    private void initUtils(Map props) {
        ApplicationContext ac = AppUtil.getApplicationContext();
        
        wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");
        workflowManager = (WorkflowManager) ac.getBean("workflowManager");
    }
    
    /**
     * Used to validate necessary input values prior to executing API calls. This method is wrapped by load().
     * @return A boolean value to continue or skip plugin execution. Default value is true.
     */
    public boolean isInputDataValid() {
        return true;
    }
    
    /**
     * HTML template for front-end UI. This method is wrapped by renderTemplate().
     * @param formData
     * @param dataModel Model containing values to be displayed in the template.
     * @return A string representing the HTML element to render
     */
    public abstract String renderElement(FormData formData, Map dataModel, Client client);
    
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {        
        initUtils(getProperties());
        
        if (!isInputDataValid()) {
            LogUtil.debug(getClassName(), "Invalid input(s) detected. Aborting plugin execution.");
            return null;
        }
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        try {
            if (FormUtil.isFormBuilderActive()) {
                return renderElement(formData, dataModel, null);
            }
            
            final Client client = BackendUtil.getHederaClient(getProperties());

            if (client == null) {
                LogUtil.warn(getClassName(), "Unable to initialize hedera client. Aborting plugin execution.");
                return "";
            }
            
            return renderElement(formData, dataModel, client);
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error executing form element plugin...");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        
        return "";
    }
    
    @Override
    public String getFormBuilderCategory() {
        return PluginUtil.FORM_ELEMENT_CATEGORY;
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
