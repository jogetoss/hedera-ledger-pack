package org.joget.hedera.lib.hashvariable;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HbarUnit;
import com.hedera.hashgraph.sdk.TransactionId;
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

public class HederaTransactionHashVariable extends HederaHashVariable {
    @Override
    protected String processHashVariable(Client client, String variableKey) {
        if (!variableKey.contains("[") || !variableKey.contains("]")) {
            return null;
        }
        
        // Retrieve Tx ID from variableKey
        final String transactionId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
        
        // Check for valid Tx ID before proceeding
        try {
            TransactionId.fromString(transactionId);
        } catch (Exception e) {
            LogUtil.debug(getClassName(), "Invalid transaction ID of --> " + transactionId);
            return null;
        }
        
        final String attribute = variableKey.replace("[" + transactionId + "]", "").replace(".", "");
        
        //If same tx ID is already loaded on the existing context, read from cached request instead
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        final String txAttrKey = transactionId + "-txHashVar";
        
        JSONObject jsonResponse;
        if (request.getAttribute(txAttrKey) != null) {
            jsonResponse = (JSONObject) request.getAttribute(txAttrKey);
        } else {
            final MirrorRestService restService = new MirrorRestService(getProperties(), client.getLedgerId());
            jsonResponse = restService.getTxData(
                    (
                        transactionId.substring(0, transactionId.lastIndexOf("."))
                        + "-"
                        + transactionId.substring(transactionId.lastIndexOf(".") + 1)
                    ).replace("@", "-")
            );
            if (jsonResponse == null) {
                LogUtil.warn(getClassName(), "Error retrieving data from mirror node.");
                return null;
            }
            request.setAttribute(txAttrKey, jsonResponse);
        }
        
        jsonResponse = jsonResponse.getJSONArray("transactions").getJSONObject(0);
        
        try {
            return switch (attribute) {
                case "assessedCustomFees" -> 
                    jsonResponse.has("assessed_custom_fees") && !jsonResponse.isNull("assessed_custom_fees")
                            ? String.valueOf(jsonResponse.getJSONArray("assessed_custom_fees"))
                            : "none";
                case "chargedTxFee" -> 
                    Hbar.from(jsonResponse.getBigDecimal("charged_tx_fee"), HbarUnit.TINYBAR)
                            .toString(HbarUnit.HBAR);
                case "consensusTimestamp" -> epochTimetoDate(jsonResponse.getString("consensus_timestamp"));
                case "entityId" -> 
                    jsonResponse.has("entity_id") && !jsonResponse.isNull("entity_id")
                            ? jsonResponse.getString("entity_id")
                            : "none";
                case "maxFee" ->
                    Hbar.fromString(jsonResponse.getString("max_fee"), HbarUnit.TINYBAR)
                            .toString(HbarUnit.HBAR);
                case "memo" -> 
                    jsonResponse.has("memo_base64") && !jsonResponse.isNull("memo_base64")
                            ? new String(Base64.getDecoder().decode(jsonResponse.getString("memo_base64")))
                            : "";
                case "txType" -> jsonResponse.getString("name");
                case "nftTransfers" -> String.valueOf(jsonResponse.getJSONArray("nft_transfers"));
                case "node" -> jsonResponse.getString("node");
                case "nonce" -> String.valueOf(jsonResponse.getInt("nonce"));
                case "parentConsensusTimestamp" -> 
                    jsonResponse.has("parent_consensus_timestamp") && !jsonResponse.isNull("parent_consensus_timestamp")
                            ? epochTimetoDate(jsonResponse.getString("parent_consensus_timestamp"))
                            : "none";
                case "result" -> jsonResponse.getString("result");
                case "isScheduled" -> String.valueOf(jsonResponse.getBoolean("scheduled"));
                case "stakingRewardTransfers" -> String.valueOf(jsonResponse.getJSONArray("staking_reward_transfers"));
                case "txHash" -> jsonResponse.getString("transaction_hash");
                case "tokenTransfers" -> String.valueOf(jsonResponse.getJSONArray("token_transfers"));
                case "transfers" -> String.valueOf(jsonResponse.getJSONArray("transfers"));
                case "validDurationSeconds" -> jsonResponse.getString("valid_duration_seconds");
                case "validStartTimestamp" -> epochTimetoDate(jsonResponse.getString("valid_start_timestamp"));
                default -> null;
            };
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error retrieving transaction attribute " + attribute);
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
        return "hedera-tx";
    }
    
    @Override
    public Collection<String> availableSyntax() {
        final String syntaxPrefix = getPrefix() + ".[TRANSACTION_ID].";
        
        Collection<String> syntax = new ArrayList<String>();
        syntax.add(syntaxPrefix + "assessedCustomFees");
        syntax.add(syntaxPrefix + "chargedTxFee");
        syntax.add(syntaxPrefix + "consensusTimestamp");
        syntax.add(syntaxPrefix + "entityId");
        syntax.add(syntaxPrefix + "maxFee");
        syntax.add(syntaxPrefix + "memo");
        syntax.add(syntaxPrefix + "txType");
        syntax.add(syntaxPrefix + "nftTransfers");
        syntax.add(syntaxPrefix + "node");
        syntax.add(syntaxPrefix + "nonce");
        syntax.add(syntaxPrefix + "parentConsensusTimestamp");
        syntax.add(syntaxPrefix + "result");
        syntax.add(syntaxPrefix + "isScheduled");
        syntax.add(syntaxPrefix + "stakingRewardTransfers");
        syntax.add(syntaxPrefix + "txHash");
        syntax.add(syntaxPrefix + "tokenTransfers");
        syntax.add(syntaxPrefix + "transfers");
        syntax.add(syntaxPrefix + "validDurationSeconds");
        syntax.add(syntaxPrefix + "validStartTimestamp");

        return syntax;
    }
    
    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClassName(), "/properties/hashvariable/HederaTransactionHashVariable.json", null, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Load transaction data from the Hedera DLT.";
    }

    @Override
    public String getName() {
        return "Hedera Transaction Hash Variable";
    }
}
