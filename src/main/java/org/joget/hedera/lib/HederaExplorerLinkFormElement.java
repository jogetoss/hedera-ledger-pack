package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.Client;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormContainer;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaFormElementAbstract;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.ExplorerUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TokenUtil;
import org.joget.hedera.service.TransactionUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaExplorerLinkFormElement extends HederaFormElementAbstract implements FormContainer {

    private static final String TX_ID_TYPE = "transactionId";
    private static final String ADDRESS_TYPE = "accountAddress";
    private static final String TOKEN_TYPE = "tokenId";
    
    private Client client;
    
    @Override
    public String getName() {
        return "Hedera Explorer Link";
    }

    @Override
    public String getDescription() {
        return "A clickable button or link in a form to navigate to several popular public Hedera explorers to verify information.";
    }
    
    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        return AppUtil.readPluginResource(
                getClassName(), 
                "/properties/HederaExplorerLinkFormElement.json", 
                new String[]{backendConfigs}, 
                true, 
                PluginUtil.MESSAGE_PATH
        );
    }

    @Override
    public String renderElement(FormData formData, Map dataModel, Client client) {
        this.client = client;
        
        String explorerType = getPropertyString("explorerType");
        String valueType = getPropertyString("valueType");
        String getValueMode = getPropertyString("getValueMode");
        
        String retrievedValue;
        
        if (FormUtil.isFormBuilderActive()) { // Don't need to unnecessarily retrieve value when in Form Builder
            retrievedValue = "";
        } else {
            switch (getValueMode) {
                case "fieldId" :
                    String fieldId = getPropertyString("getFieldId");

                    Form form = FormUtil.findRootForm(this);
                    Element fieldElement = FormUtil.findElement(fieldId, form, formData);

                    retrievedValue = FormUtil.getElementPropertyValue(fieldElement, formData);
                    break;
                case "hashVariable" :
                    String textHashVariable = getPropertyString("textHashVariable");
                    retrievedValue = WorkflowUtil.processVariable(textHashVariable, "", wfAssignment);
                    break;
                default:
                    String workflowVariable = getPropertyString("workflowVariable");
                    retrievedValue = workflowManager.getProcessVariable(wfAssignment.getProcessId(), workflowVariable);
                    break;
            }
        }
        
        dataModel.put("element", this);
        dataModel.put("isValidValue", checkValueExist(valueType, retrievedValue));
        dataModel.put("explorerUrl", getExplorerUrl(valueType, retrievedValue, explorerType));
        
        return FormUtil.generateElementHtml(this, formData, "HederaExplorerLinkFormElement.ftl", dataModel);
    }
    
    public boolean checkValueExist(String valueType, String retrievedValue) {
        if (retrievedValue == null || retrievedValue.isBlank() || FormUtil.isFormBuilderActive()) {
            return false;
        }
        
        try {
            switch (valueType) {
                case ADDRESS_TYPE :
                    return AccountUtil.isAccountExist(client, retrievedValue);
                case TOKEN_TYPE :
                    return TokenUtil.isTokenExist(client, retrievedValue);
                case TX_ID_TYPE:
                default:
                    return TransactionUtil.isTransactionExist(getProperties(), retrievedValue);
            }
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error retrieving on-chain data.");
        }
        
        return false;
    }
    
    public String getExplorerUrl(String valueType, String retrievedValue, String explorerType) {
        if (FormUtil.isFormBuilderActive()) {
            return "";
        }
        
        switch (valueType) {
            case ADDRESS_TYPE :
                return ExplorerUtil.getAddressUrl(getProperties(), retrievedValue, explorerType);
            case TOKEN_TYPE :
                return ExplorerUtil.getTokenUrl(getProperties(), retrievedValue, explorerType);
            case TX_ID_TYPE:
            default:
                return ExplorerUtil.getTransactionUrl(getProperties(), retrievedValue, explorerType);
        }
    }
    
    @Override
    public int getFormBuilderPosition() {
        return 1;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fas fa-external-link-alt\"></i>";
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<span class='form-floating-label'>Hedera Explorer Link</span>";
    }
}
