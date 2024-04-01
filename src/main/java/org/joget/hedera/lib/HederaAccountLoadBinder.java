package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HbarUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadElementBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaFormBinder;
import org.joget.hedera.service.MirrorRestService;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
* @deprecated
* For more flexibility in app design, use the various Hash Variable plugins available in this plugin pack to retrieve data from the Hedera mirror node.
*/
@Deprecated
public class HederaAccountLoadBinder extends HederaFormBinder implements FormLoadElementBinder {

    @Override
    public String getName() {
        return "Hedera Account Load Binder";
    }

    @Override
    public String getDescription() {
        return "Load account data from the Hedera DLT into a form.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaAccountLoadBinder.json", new String[]{backendConfigs}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid() {
        final String accountId = WorkflowUtil.processVariable(getPropertyString("accountId"), "", null);

        //Prevent error thrown from empty value and invalid hash variable
        return !accountId.isEmpty() && !accountId.startsWith("#");
    }
    
    @Override
    protected FormRowSet loadData(Client client, Element element, String primaryKey, FormData formData) 
            throws TimeoutException, RuntimeException {
        
        final String accountId = WorkflowUtil.processVariable(getPropertyString("accountId"), "", null);
            
        final MirrorRestService restService = new MirrorRestService(getProperties(), client.getLedgerId());
        JSONObject jsonResponse = restService.get("accounts/" + accountId);
        if (jsonResponse == null) {
            LogUtil.warn(getClassName(), "Error retrieving data from mirror node.");
        }

        String hbarBalanceField = getPropertyString("hbarBalanceField");
        Object[] tokenBalances = (Object[]) getProperty("tokenBalances");
        String accountMemoField = getPropertyString("accountMemoField");
        String accountIsDeletedField = getPropertyString("accountIsDeletedField");
        String receiverSignatureRequiredField = getPropertyString("receiverSignatureRequiredField");
        String evmAddressField = getPropertyString("evmAddressField");
        String maxAutoTokenAssociationsField = getPropertyString("maxAutoTokenAssociationsField");

        FormRow row = new FormRow();
        row = addRow(
                row,
                hbarBalanceField,
                Hbar.from(
                        jsonResponse.getJSONObject("balance").getBigDecimal("balance"), 
                        HbarUnit.TINYBAR
                ).toString(HbarUnit.HBAR)
        );
        JSONArray accountTokens = jsonResponse.getJSONObject("balance").getJSONArray("tokens");
        for (Object o : tokenBalances) {
            Map mapping = (HashMap) o;
            String tokenId = mapping.get("tokenId").toString();
            String formFieldId = mapping.get("formFieldId").toString();

            if (accountTokens.isEmpty()) {
                row = addRow(row, formFieldId, "No balance found");
                continue;
            }

            for (int i = 0; i < accountTokens.length(); i++) {
                JSONObject balanceObj = accountTokens.getJSONObject(i);
                if (balanceObj.getString("token_id").equals(tokenId)) {
                    row = addRow(row, formFieldId, String.valueOf(balanceObj.getBigDecimal("balance")));
                } else {
                    row = addRow(row, formFieldId, "No balance found");
                }
            }
        }
        row = addRow(row, accountMemoField, jsonResponse.getString("memo"));
        row = addRow(row, accountIsDeletedField, String.valueOf(jsonResponse.getBoolean("deleted")));
        row = addRow(row, receiverSignatureRequiredField, String.valueOf(jsonResponse.getBoolean("receiver_sig_required")));
        row = addRow(row, evmAddressField, jsonResponse.getString("evm_address"));
        row = addRow(row, maxAutoTokenAssociationsField, String.valueOf(jsonResponse.getInt("max_automatic_token_associations")));

        FormRowSet rows = new FormRowSet();
        rows.add(row);

        return rows;
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        
        return row;
    }
}
