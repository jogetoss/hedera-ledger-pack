package org.joget.hedera.lib.hashvariable;

import com.hedera.hashgraph.sdk.AccountId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaHashVariable;
import org.joget.hedera.service.MirrorRestService;
import org.joget.hedera.service.PluginUtil;
import org.json.JSONObject;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HbarUnit;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;

public class HederaAccountHashVariable extends HederaHashVariable {

    @Override
    protected String processHashVariable(Client client, String variableKey) {
        if (!variableKey.contains("[") || !variableKey.contains("]")) {
            return null;
        }
        
        // Retrieve Account ID from variableKey
        final String accountID = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
        
        // Check for valid Account ID before proceeding
        try {
            AccountId.fromString(accountID);
        } catch (Exception e) {
            LogUtil.debug(getClassName(), "Invalid account ID of --> " + accountID);
            return null;
        }
        
        variableKey = variableKey.replace("[" + accountID + "]", "");

        // Retrieve Token ID if exist in variableKey
        String tokenID = "";
        if (variableKey.toLowerCase().contains("tokenbalance")) {
            if (!variableKey.contains("[") || !variableKey.contains("]")) {
                return null;
            }
            tokenID = variableKey.substring(variableKey.lastIndexOf("[") + 1, variableKey.lastIndexOf("]"));
            variableKey = variableKey.replace("[" + tokenID + "]", "");
        }
        
        final String attribute = variableKey.replace(".", "");

        final JSONObject jsonResponse = getData(client, accountID);
        
        if (jsonResponse.has("_status") && jsonResponse.getJSONObject("_status").getJSONArray("messages").getJSONObject(0).getString("message").equals("Not found")) {
            return "Account ID does not exist";
        }

        try {
            return switch (attribute.toLowerCase()) {
                case "evmaddress" -> jsonResponse.getString("evm_address");
                case "accountmemo" -> jsonResponse.getString("memo");
                case "isaccountdeleted" -> String.valueOf(jsonResponse.getBoolean("deleted"));
                case "receiversignaturerequired" -> String.valueOf(jsonResponse.getBoolean("receiver_sig_required"));
                case "maxautotokenassociations" -> String.valueOf(jsonResponse.getInt("max_automatic_token_associations"));
                case "declinereward" -> String.valueOf(jsonResponse.getBoolean("decline_reward"));
                case "ethereumnonce" -> String.valueOf(jsonResponse.getInt("ethereum_nonce"));
                case "autorenewperiod" -> epochRenewPeriod(String.valueOf(jsonResponse.getInt("auto_renew_period")));
                case "createdtimestamp" -> epochTimetoDate(jsonResponse.getString("created_timestamp"));
                case "expirytimestamp" -> epochTimetoDate(jsonResponse.getString("expiry_timestamp"));
                case "publickeytype" -> jsonResponse.getJSONObject("key").getString("_type");
                case "publickey" -> jsonResponse.getJSONObject("key").getString("key");
                case "pendingreward" -> String.valueOf(jsonResponse.getInt("pending_reward"));
                case "alltokens" -> String.valueOf(jsonResponse.getJSONObject("balance").getJSONArray("tokens"));
                case "hbarbalance" ->
                    Hbar.from(jsonResponse.getJSONObject("balance").getBigDecimal("balance"), HbarUnit.TINYBAR)
                            .toString(HbarUnit.HBAR);
                case "allowances" -> 
                    jsonResponse.has("allowances") && !jsonResponse.isNull("allowances")
                            ? jsonResponse.getString("allowances")
                            : "Does Not Exist";
                case "alias" -> 
                    jsonResponse.has("alias") && !jsonResponse.isNull("alias")
                            ? jsonResponse.getString("alias")
                            : "Does Not Exist";
                case "rewards" -> 
                    jsonResponse.has("rewards") && !jsonResponse.isNull("rewards")
                            ? jsonResponse.getString("rewards")
                            : "Does Not Exist";
                case "stakedaccountid" ->
                    jsonResponse.has("staked_account_id") && !jsonResponse.isNull("staked_account_id")
                            ? jsonResponse.getString("staked_account_id")
                            : "Does Not Exist";
                case "stakednodeid" -> 
                    jsonResponse.has("staked_node_id") && !jsonResponse.isNull("staked_node_id")
                            ? jsonResponse.getString("staked_node_id")
                            : "Does Not Exist";
                case "stakeperiodstart" ->
                    jsonResponse.has("staked_period_start") && !jsonResponse.isNull("staked_period_start")
                            ? jsonResponse.getString("staked_period_start")
                            : "Does Not Exist";
                case "tokenbalance" -> {
                    JSONArray accountTokens = jsonResponse.getJSONObject("balance").getJSONArray("tokens");

                    for (int i = 0; i < accountTokens.length(); i++) {
                        JSONObject token = accountTokens.getJSONObject(i);
                        if ((token.getString("token_id")).equals(tokenID)) {
                            yield String.valueOf(token.getInt("balance"));
                        }
                    }

                    yield "Does Not Exist";
                }
                default -> null;
            };
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error retrieving account attribute " + attribute);
        }

        return null;
    }

    private String epochRenewPeriod(String epochTime) {
        try {
            if (!epochTime.contains(".")) {
                long days = Duration
                        .ofSeconds(Integer.parseInt(epochTime))
                        .toDays();

                return days + " Days";
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Epoch Time Error " + epochTime);
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
    private JSONObject getData(Client client, String accountId) {
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        final String attrKey = accountId + "-accountHashVar";
        
        if (request.getAttribute(attrKey) != null) {
            return (JSONObject) request.getAttribute(attrKey);
        }
        
        JSONObject jsonResponse = new MirrorRestService(getProperties(), client.getLedgerId()).getAccountData(accountId);
        request.setAttribute(attrKey, jsonResponse);
        
        return jsonResponse;
    }
    
    @Override
    public String getPrefix() {
        return "hedera-account";
    }

    @Override
    public Collection<String> availableSyntax() {
        final String syntaxPrefix = getPrefix() + ".[ACCOUNT_ID].";
        
        Collection<String> syntax = new ArrayList<String>();
        syntax.add(syntaxPrefix + "allowances");
        syntax.add(syntaxPrefix + "allTokens");
        syntax.add(syntaxPrefix + "tokenBalance");
        syntax.add(syntaxPrefix + "hbarBalance");
        syntax.add(syntaxPrefix + "accountMemo");
        syntax.add(syntaxPrefix + "isAccountDeleted");
        syntax.add(syntaxPrefix + "receiverSignatureRequired");
        syntax.add(syntaxPrefix + "evmAddress");
        syntax.add(syntaxPrefix + "maxAutoTokenAssociations");
        syntax.add(syntaxPrefix + "alias");
        syntax.add(syntaxPrefix + "autoRenewPeriod");
        syntax.add(syntaxPrefix + "createdTimestamp");
        syntax.add(syntaxPrefix + "declineReward");
        syntax.add(syntaxPrefix + "ethereumNonce");
        syntax.add(syntaxPrefix + "expiryTimestamp");
        syntax.add(syntaxPrefix + "publicKeyType");
        syntax.add(syntaxPrefix + "publicKey");
        syntax.add(syntaxPrefix + "pendingReward");
        syntax.add(syntaxPrefix + "rewards");
        syntax.add(syntaxPrefix + "stakedAccountID");
        syntax.add(syntaxPrefix + "stakedNodeID");
        syntax.add(syntaxPrefix + "stakePeriodStart");

        return syntax;
    }

    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClassName(), "/properties/hashvariable/HederaAccountHashVariable.json", null, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Load various account values from the Hedera DLT.";
    }

    @Override
    public String getName() {
        return "Hedera Account Hash Variable";
    }
}
