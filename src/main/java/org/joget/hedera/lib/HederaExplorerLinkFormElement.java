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
import org.joget.hedera.model.HederaFormElement;
import org.joget.hedera.model.NetworkType;
import org.joget.hedera.model.explorer.Explorer;
import org.joget.hedera.model.explorer.ExplorerFactory;
import org.joget.hedera.service.BackendUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class HederaExplorerLinkFormElement extends HederaFormElement implements FormContainer {

    private static final String TX_ID_TYPE = "transactionId";
    private static final String ADDRESS_TYPE = "accountAddress";
    private static final String TOKEN_TYPE = "tokenId";
    private static final String TOPIC_TYPE = "topicId";
    
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
        
        final NetworkType networkType = BackendUtil.getNetworkType(getProperties());
        
        try {
            switch (valueType) {
                case ADDRESS_TYPE : {
                    JSONObject jsonResponse = BackendUtil.httpGet(
                            networkType.getMirrorNodeUrl() + "accounts/" + retrievedValue
                    );
                    return (jsonResponse != null && (jsonResponse.getString("account")).equals(retrievedValue));
                }
                case TOKEN_TYPE : {
                    JSONObject jsonResponse = BackendUtil.httpGet(
                            networkType.getMirrorNodeUrl() + "tokens/" + retrievedValue
                    );
                    return (jsonResponse != null && (jsonResponse.getString("token_id")).equals(retrievedValue));
                }
                case TX_ID_TYPE: {
                    String formattedTxId = retrievedValue.replaceAll("@", "-");
                    formattedTxId = formattedTxId.substring(0, formattedTxId.lastIndexOf(".")) + "-" + formattedTxId.substring(formattedTxId.lastIndexOf(".") + 1);
                    
                    JSONObject jsonResponse = BackendUtil.httpGet(
                            networkType.getMirrorNodeUrl() + "transactions/" + formattedTxId
                    );
                    return (jsonResponse != null && (jsonResponse.getJSONArray("transactions").getJSONObject(0) != null));
                }
                case TOPIC_TYPE: {
                    JSONObject jsonResponse = BackendUtil.httpGet(
                            networkType.getMirrorNodeUrl() + "topics/" + retrievedValue + "/messages?encoding=utf-8"
                    );
                    return (jsonResponse != null && (jsonResponse.getJSONArray("messages").getJSONObject(0) != null));
                }
                default:
                    LogUtil.warn(getClassName(), "Unknown explorer function selection found!");
                    return false;
            }
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error retrieving data from mirror node.");
        }
        
        return false;
    }
    
    public String getExplorerUrl(String valueType, String retrievedValue, String explorerType) {
        if (FormUtil.isFormBuilderActive() || retrievedValue.isBlank()) {
            return "";
        }
        
        final NetworkType networkType = BackendUtil.getNetworkType(getProperties());
        Explorer explorer = new ExplorerFactory(networkType).createExplorer(explorerType);
        
        switch (valueType) {
            case ADDRESS_TYPE :
                return explorer.getAccountUrl(retrievedValue);
            case TOKEN_TYPE :
                return explorer.getTokenUrl(retrievedValue);
            case TX_ID_TYPE:
                return explorer.getTransactionUrl(retrievedValue);
            case TOPIC_TYPE:
                return explorer.getTopicUrl(retrievedValue);
            default:
                LogUtil.warn(getClassName(), "Unknown explorer function selection found!");
                return null;
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
        return "<div class=\"form-cell\"><span class='form-floating-label'>Hedera Explorer Link</span></div>";
    }
}
