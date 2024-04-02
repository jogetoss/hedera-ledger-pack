package org.joget.hedera.model;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.explorer.Explorer;
import org.joget.hedera.model.explorer.ExplorerFactory;
import static org.joget.hedera.model.explorer.ExplorerFactory.DEFAULT_EXPLORER;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

public abstract class HederaProcessTool extends DefaultApplicationPlugin {
    
    protected AppService appService;
    protected AppDefinition appDef;
    protected WorkflowAssignment wfAssignment;
    protected WorkflowManager workflowManager;
    
    protected Client client;
    
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
     * Used to validate necessary input values prior to executing main logic. This method is wrapped by execute().
     * @return A boolean value to continue or abort plugin execution. Default value is true.
     */
    public boolean isInputDataValidWithClient(Map props, Client client) 
            throws TimeoutException, RuntimeException {
        return true;
    }
    
    /**
     * To execute logic in a process tool. This method is wrapped by execute().
     * 
     * A org.joget.workflow.model.WorkflowAssignment object is passed as "workflowAssignment" property when it is available.
     * 
     * @param props
     * @return is not used for now
     */
    protected abstract Object runTool(Map props) 
            throws TimeoutException, RuntimeException;
    
    @Override
    public Object execute(Map props) {
        initUtils(props);
        
        if (!isInputDataValid(props)) {
            LogUtil.debug(getClassName(), "Invalid input(s) detected. Aborting plugin execution.");
            return null;
        }
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        try (final Client tempClient = BackendUtil.getHederaClient(props)) {
            if (tempClient == null) {
                LogUtil.warn(getClassName(), "Unable to initialize hedera client. Aborting plugin execution.");
                return null;
            }
            
            this.client = tempClient;
            
            if (!isInputDataValidWithClient(props, client)) {
                LogUtil.debug(getClassName(), "Invalid input(s) detected. Aborting plugin execution.");
                return null;
            }

            return runTool(props);
        } catch (TimeoutException ex) {
            LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to timeout.");
        } catch (RuntimeException ex) { //Compatibility workaround for MultiTenantPluginManager - avoid using SDK's custom exceptions
            final String exceptionMessage = ex.getMessage();
            
            if (exceptionMessage.contains("PrecheckStatusException")) {
                LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to failed transaction prechecks.");
            } else if (exceptionMessage.contains("BadMnemonicException")) {
                LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to bad mnemonic inputted.");
            } else if (exceptionMessage.contains("ReceiptStatusException")) {
                LogUtil.error(getClassName(), ex, "Error executing process tool plugin due to failed transaction.");
            } else {
                LogUtil.error(getClassName(), ex, "Unhandled RuntimeException occured...");
            }
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error executing process tool plugin...");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        
        return null;
    }
    
    protected FormRowSet getFormRecord(String formDefId, String primaryKey) {        
        //If no primary key defined, attempt to retrieve from process instance context
        if (primaryKey == null || primaryKey.isBlank()) {
            primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        }
        
        return appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, primaryKey);
    }
    
    protected FormRowSet storeFormRow(String formDefId, FormRow formRow) {
        FormRowSet rowSet = new FormRowSet();
        rowSet.add(formRow);
        
        return appService.storeFormData(appDef.getId(), appDef.getVersion().toString(), formDefId, rowSet, null);
    }
    
    protected void storeGenericTxDataToWorkflowVariable(TransactionRecord transactionRecord) {
        
        String wfTransactionValidated = getPropertyString("wfTransactionValidated");
        String wfConsensusTimestamp = getPropertyString("wfConsensusTimestamp");
        String wfTransactionId = getPropertyString("wfTransactionId");
        String wfTransactionExplorerUrl = getPropertyString("wfTransactionExplorerUrl");
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfTransactionValidated, 
                transactionRecord.receipt.status.toString()
        );
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfConsensusTimestamp, 
                TransactionUtil.convertInstantToZonedDateTimeString(transactionRecord.consensusTimestamp)
        );
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfTransactionId, 
                transactionRecord.transactionId.toString()
        );
        
        Explorer explorer = new ExplorerFactory(client.getLedgerId()).createExplorer(DEFAULT_EXPLORER);
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfTransactionExplorerUrl, 
                transactionRecord.transactionId.toString() != null ? explorer.getTransactionUrl(transactionRecord.transactionId.toString()) : "Not available"
        );
    }
    
    protected void storeValuetoActivityVar(String activityId, String variable, String value) {
        if (activityId == null || activityId.isEmpty() || variable.isEmpty() || value == null) {
            return;
        }
        
        workflowManager.activityVariable(activityId, variable, value);
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
