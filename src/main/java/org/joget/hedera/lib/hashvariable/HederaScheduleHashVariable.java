package org.joget.hedera.lib.hashvariable;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ScheduleId;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaHashVariable;
import org.joget.hedera.service.MirrorRestService;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class HederaScheduleHashVariable extends HederaHashVariable {
    
    @Override
    protected String processHashVariable(Client client, String variableKey) {
        if (!variableKey.contains("[") || !variableKey.contains("]")) {
            return null;
        }
        
        // Retrieve Schedule ID from variableKey
        final String scheduleId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
        
        // Check for valid Schedule ID before proceeding
        try {
            ScheduleId.fromString(scheduleId);
        } catch (Exception e) {
            LogUtil.debug(getClassName(), "Invalid schedule ID of --> " + scheduleId);
            return null;
        }
        
        final String attribute = variableKey.replace("[" + scheduleId + "]", "").replace(".", "");
        
        final JSONObject jsonResponse = getData(client, scheduleId);
        
        if (jsonResponse.has("_status") && jsonResponse.getJSONObject("_status").getJSONArray("messages").getJSONObject(0).getString("message").equals("Not found")) {
            return "Scheduled transaction does not exist";
        }
        
        try {
            return switch (attribute) {
                case "adminKey" -> 
                    !jsonResponse.isNull("admin_key")
                            ? String.valueOf(jsonResponse.getJSONObject("admin_key"))
                            : "none";
                case "consensusTimestamp" -> epochTimeToDate(jsonResponse.getString("consensus_timestamp"));
                case "creatorAccountId" -> jsonResponse.getString("creator_account_id");
                case "isDeleted" -> String.valueOf(jsonResponse.getBoolean("deleted"));
                case "executedTimestamp" -> 
                    !jsonResponse.isNull("executed_timestamp")
                            ? epochTimeToDate(jsonResponse.getString("executed_timestamp"))
                            : "none";
                case "expirationTime" -> 
                    !jsonResponse.isNull("expiration_time")
                            ? epochTimeToDate(jsonResponse.getString("expiration_time"))
                            : "none";
                case "memo" -> jsonResponse.getString("memo");
                case "payerAccountId" -> jsonResponse.getString("payer_account_id");
                case "signatures" -> String.valueOf(jsonResponse.getJSONArray("signatures"));
                case "transactionBody" -> jsonResponse.getString("transaction_body");
                case "waitForExpiry" -> String.valueOf(jsonResponse.getBoolean("wait_for_expiry"));
                default -> null;
            };
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error retrieving schedule attribute " + attribute);
        }
        
        return null;
    }
    
    private String epochTimeToDate(String epochTime) {
        try {
            return new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a z")
                    .format(new Date((long) Double.parseDouble(epochTime) * 1000))
                    .toUpperCase();
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Epoch Time Error " + epochTime);
        }

        return null;
    }
    
    //If same value already loaded on the existing context, read from cached request instead
    private JSONObject getData(Client client, String scheduleId) {
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        final String attrKey = scheduleId + "-scheduleHashVar";
        
        if (request.getAttribute(attrKey) != null) {
            return (JSONObject) request.getAttribute(attrKey);
        }
        
        JSONObject jsonResponse = new MirrorRestService(getProperties(), client.getLedgerId()).getScheduleData(scheduleId);
        request.setAttribute(attrKey, jsonResponse);
        
        return jsonResponse;
    }
    
    @Override
    public String getPrefix() {
        return "hedera-schedule";
    }
    
    @Override
    public Collection<String> availableSyntax() {
        final String syntaxPrefix = getPrefix() + ".[SCHEDULE_ID].";
        
        Collection<String> syntax = new ArrayList<String>();
        syntax.add(syntaxPrefix + "adminKey");
        syntax.add(syntaxPrefix + "consensusTimestamp");
        syntax.add(syntaxPrefix + "creatorAccountId");
        syntax.add(syntaxPrefix + "isDeleted");
        syntax.add(syntaxPrefix + "executedTimestamp");
        syntax.add(syntaxPrefix + "expirationTime");
        syntax.add(syntaxPrefix + "memo");
        syntax.add(syntaxPrefix + "payerAccountId");
        syntax.add(syntaxPrefix + "signatures");
        syntax.add(syntaxPrefix + "transactionBody");
        syntax.add(syntaxPrefix + "waitForExpiry");

        return syntax;
    }
    
    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClassName(), "/properties/hashvariable/HederaScheduleHashVariable.json", null, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Load scheduled transaction data from the Hedera DLT.";
    }

    @Override
    public String getName() {
        return "Hedera Schedule Hash Variable";
    }
}
