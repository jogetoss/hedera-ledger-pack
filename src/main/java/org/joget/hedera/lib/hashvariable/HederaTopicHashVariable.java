package org.joget.hedera.lib.hashvariable;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TopicId;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaHashVariable;
import org.joget.hedera.service.MirrorRestService;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class HederaTopicHashVariable extends HederaHashVariable {
    
    @Override
    protected String processHashVariable(Client client, String variableKey) {
        if (!variableKey.contains("[") || !variableKey.contains("]")) {
            return null;
        }
        
        // Retrieve Topic ID from variableKey
        final String topicId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
        
        // Check for valid Topic ID before proceeding
        try {
            TopicId.fromString(topicId);
        } catch (Exception e) {
            LogUtil.debug(getClassName(), "Invalid topic ID of --> " + topicId);
            return null;
        }
        
        variableKey = variableKey.replace("[" + topicId + "]", "");
        
        // Retrieve Sequence Number if exist in variableKey
        String sequenceNumber = "";
        if (variableKey.startsWith(".message")) {
            if (!variableKey.contains("[") || !variableKey.contains("]")) {
                return null;
            }
            sequenceNumber = variableKey.substring(variableKey.lastIndexOf("[") + 1, variableKey.lastIndexOf("]"));
            variableKey = variableKey.replace("message[" + sequenceNumber + "]", "");
        }
        
        final String attribute = variableKey.replace(".", "");
        
        JSONObject jsonResponse;
        if (attribute.equals("allMessages")) {
            jsonResponse = getAllMessages(client, topicId);
            
            if (jsonResponse.has("_status") && jsonResponse.getJSONObject("_status").getJSONArray("messages").getJSONObject(0).getString("message").equals("Not found")) {
                return "Topic ID does not exist";
            }
        } else {
            jsonResponse = getMessageData(client, topicId, sequenceNumber);
            
            if (jsonResponse.has("_status") && jsonResponse.getJSONObject("_status").getJSONArray("messages").getJSONObject(0).getString("message").equals("Not found")) {
                return "Topic message does not exist";
            }
        }
        
        try {
            return switch (attribute) {
                case "allMessages" -> String.valueOf(jsonResponse.getJSONArray("messages"));
                case "consensusTimestamp" -> epochTimetoDate(jsonResponse.getString("consensus_timestamp"));
                case "messageContent" -> new String(Base64.getDecoder().decode(jsonResponse.getString("message")));
                case "payerAccountId" -> jsonResponse.getString("payer_account_id");
                case "runningHash" -> jsonResponse.getString("running_hash");
                case "runningHashVersion" -> String.valueOf(jsonResponse.getInt("running_hash_version"));
                default -> null;
            };
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error retrieving topic attribute " + attribute);
        }
        
        return null;
    }
    
    private String epochTimetoDate(String epochTime) {
        try {
            if (epochTime.contains(".")) {
                return new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a z")
                        .format(new java.util.Date((long) Double.parseDouble(epochTime) * 1000))
                        .toUpperCase();
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Epoch Time Error " + epochTime);
        }

        return null;
    }
    
    //If same value already loaded on the existing context, read from cached request instead
    private JSONObject getAllMessages(Client client, String topicId) {
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        final String attrKey = topicId + "-topicHashVar";
        
        if (request.getAttribute(attrKey) != null) {
            return (JSONObject) request.getAttribute(attrKey);
        }
        
        JSONObject jsonResponse = new MirrorRestService(getProperties(), client.getLedgerId()).getAllTopicMessages(topicId);
        request.setAttribute(attrKey, jsonResponse);
        
        return jsonResponse;
    }
    
    //If same value already loaded on the existing context, read from cached request instead
    private JSONObject getMessageData(Client client, String topicId, String sequenceNumber) {
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        final String attrKey = topicId + "-" + sequenceNumber + "-topicMsgSeqNumHashVar";
        
        if (request.getAttribute(attrKey) != null) {
            return (JSONObject) request.getAttribute(attrKey);
        }
        
        JSONObject jsonResponse = new MirrorRestService(getProperties(), client.getLedgerId()).getTopicMessage(topicId, sequenceNumber);
        request.setAttribute(attrKey, jsonResponse);
        
        return jsonResponse;
    }
    
    @Override
    public String getPrefix() {
        return "hedera-topic";
    }
    
    @Override
    public Collection<String> availableSyntax() {
        final String syntaxPrefix = getPrefix() + ".[TOPIC_ID].";
        
        Collection<String> syntax = new ArrayList<String>();
        syntax.add(syntaxPrefix + "allMessages");
        syntax.add(syntaxPrefix + "message[SEQUENCE_NUMBER].consensusTimestamp");
        syntax.add(syntaxPrefix + "message[SEQUENCE_NUMBER].messageContent");
        syntax.add(syntaxPrefix + "message[SEQUENCE_NUMBER].payerAccountId");
        syntax.add(syntaxPrefix + "message[SEQUENCE_NUMBER].runningHash");
        syntax.add(syntaxPrefix + "message[SEQUENCE_NUMBER].runningHashVersion");

        return syntax;
    }
    
    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClassName(), "/properties/hashvariable/HederaTopicHashVariable.json", null, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Load HCS messages from the Hedera DLT.";
    }

    @Override
    public String getName() {
        return "Hedera Topic Hash Variable";
    }
}
