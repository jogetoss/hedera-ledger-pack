package org.joget.hedera.model;

import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

public abstract class HederaProcessToolAbstract extends DefaultApplicationPlugin {
    
    protected AppService appService;
    protected AppDefinition appDef;
    protected WorkflowAssignment wfAssignment;
    protected WorkflowManager workflowManager;
    
    private void initUtils(Map props) {
        ApplicationContext ac = AppUtil.getApplicationContext();
        
        appService = (AppService) ac.getBean("appService");
        appDef = (AppDefinition) props.get("appDef");
        wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");
        workflowManager = (WorkflowManager) ac.getBean("workflowManager");
    }
    
    /**
     * Used to validate necessary input values prior to executing API calls. This method is wrapped by execute().
     * @return A boolean value to continue or skip plugin execution. Default value is true.
     */
    public boolean isInputDataValid(Map props) {
        return true;
    }
    
    /**
     * To execute logic in a process tool. This method is wrapped by execute().
     * 
     * A org.joget.workflow.model.WorkflowAssignment object is passed as "workflowAssignment" property when it is available.
     * 
     * @param props
     * @param client The Hedera client to execute queries and actions
     * @param wfAssignment
     * @return is not used for now
     */
    protected abstract Object runTool(Map props, Client client) 
            throws TimeoutException, PrecheckStatusException, BadMnemonicException, ReceiptStatusException;
    
    @Override
    public Object execute(Map props) {
        initUtils(props);
        
        if (!isInputDataValid(props)) {
            LogUtil.debug(getClassName(), "Invalid input(s) detected. Aborting plugin execution.");
            return null;
        }
        
        Object result = null;
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        try {
            final Client client = BackendUtil.getHederaClient(props);
            
            if (client != null) {
                result = runTool(props, client);
            }
        } catch (TimeoutException ex) {
            LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to timeout.");
        } catch (PrecheckStatusException ex) {
            LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to failed transaction prechecks.");
        } catch (BadMnemonicException ex) {
            LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to bad mnemonic inputted.");
        } catch (ReceiptStatusException ex) {
            LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to unable to retrieve transaction receipt.");
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error executing process tool plugin...");
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
