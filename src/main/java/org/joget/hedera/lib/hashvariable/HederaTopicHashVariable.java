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
        
        //If same topic ID is already loaded on the existing context, read from cached request instead
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        final String topicAttrKey = topicId + "-topicHashVar";
        final String topicMsgSeqNumAttrKey = topicId + "-" + sequenceNumber + "-topicMsgSeqNumHashVar";
        
        JSONObject jsonResponse;
        if (attribute.equals("allMessages")) {
            if (request.getAttribute(topicAttrKey) != null) {
                jsonResponse = (JSONObject) request.getAttribute(topicAttrKey);
            } else {
                final MirrorRestService restService = new MirrorRestService(getProperties(), client.getLedgerId());
                jsonResponse = restService.getAllTopicMessages(topicId);
                if (jsonResponse == null) {
                    LogUtil.warn(getClassName(), "Error retrieving data from mirror node.");
                    return null;
                }
                request.setAttribute(topicAttrKey, jsonResponse);
            }
            
            if (jsonResponse.has("_status") && jsonResponse.getJSONObject("_status").getJSONArray("messages").getJSONObject(0).getString("message").equals("Not found")) {
                return "Topic ID does not exist";
            }
        } else {
            if (request.getAttribute(topicMsgSeqNumAttrKey) != null) {
                jsonResponse = (JSONObject) request.getAttribute(topicMsgSeqNumAttrKey);
            } else {
                final MirrorRestService restService = new MirrorRestService(getProperties(), client.getLedgerId());
                jsonResponse = restService.getTopicMessage(topicId, sequenceNumber);
                if (jsonResponse == null) {
                    LogUtil.warn(getClassName(), "Error retrieving data from mirror node.");
                    return null;
                }
                request.setAttribute(topicMsgSeqNumAttrKey, jsonResponse);
            }
            
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
