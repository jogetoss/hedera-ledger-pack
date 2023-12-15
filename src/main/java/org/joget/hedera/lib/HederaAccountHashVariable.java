package org.joget.hedera.lib;

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
import org.json.JSONArray;

public class HederaAccountHashVariable extends HederaHashVariable {

    @Override
    protected String processHashVariable(Client client, String variableKey) {

        String accountID = "";
        String tokenID = null;

        if (variableKey.contains("[") && variableKey.contains("]")) {

            // Retrieve and remove Account ID from variableKey
            accountID = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            variableKey = variableKey.replace("[" + accountID + "]", "");

            // Retrieve Token ID if exist in variableKey
            if (variableKey.toLowerCase().contains("tokenbalance")
                    && (variableKey.contains("[") && variableKey.contains("]"))) {
                tokenID = variableKey.substring(variableKey.lastIndexOf("[") + 1, variableKey.lastIndexOf("]"));
                variableKey = variableKey.replace("[" + tokenID + "]", "");
            }

            // Remove any String besides variableKey
            String temp[] = variableKey.split("\\.");
            variableKey = variableKey.substring((variableKey.indexOf(temp[1]) - 1));

            if (variableKey.isEmpty()) {
                LogUtil.debug(HederaHashVariable.class.getName(),
                        "#hedera." + "account" + "[" + accountID + "]." + variableKey + "# is NULL");
                return "";
            }

        }

        JSONObject jsonResponse = getMirrorResponse(client, accountID);
        String attribute = getAttribute(variableKey);

        if (attribute != null && !attribute.isEmpty()) {
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

                    case "allowances" -> jsonResponse.has("allowances") && !jsonResponse.isNull("allowances")
                            ? jsonResponse.get("allowances").toString()
                            : "Does Not Exist";

                    case "alias" -> jsonResponse.has("alias") && !jsonResponse.isNull("alias")
                            ? jsonResponse.get("alias").toString()
                            : "Does Not Exist";

                    case "rewards" -> jsonResponse.has("rewards") && !jsonResponse.isNull("rewards")
                            ? jsonResponse.get("rewards").toString()
                            : "Does Not Exist";

                    case "stakedaccountid" ->
                        jsonResponse.has("staked_account_id") && !jsonResponse.isNull("staked_account_id")
                                ? jsonResponse.get("staked_account_id").toString()
                                : "Does Not Exist";

                    case "stakednodeid" -> jsonResponse.has("staked_node_id") && !jsonResponse.isNull("staked_node_id")
                            ? jsonResponse.get("staked_node_id").toString()
                            : "Does Not Exist";

                    case "stakeperiodstart" ->
                        jsonResponse.has("staked_period_start") && !jsonResponse.isNull("staked_period_start")
                                ? jsonResponse.get("staked_period_start").toString()
                                : "Does Not Exist";

                    case "tokenbalance" -> {
                        JSONArray accountTokens = jsonResponse.getJSONObject("balance").getJSONArray("tokens");

                        for (int i = 0; i < accountTokens.length(); i++) {
                            JSONObject token = accountTokens.getJSONObject(i);
                            String tokenId = token.get("token_id").toString();

                            if (tokenId.equals(tokenID)) {
                                yield token.get("balance").toString();
                            }
                        }
                        yield "Does Not Exist";
                    }
                    default -> null;
                };
            } catch (Exception e) {
                LogUtil.error(HederaAccountHashVariable.class.getName(), e,
                        "Error retrieving user attribute " + attribute);
            }
        }

        return null;

    }

    private String getAttribute(String variableKey) {

        // Detect the attribute in variableKey
        for (String v : availableSyntax()) {
            v = v.replaceAll("hedera.account", "");

            if (variableKey != null && variableKey.equalsIgnoreCase(v)) {
                return v.substring(1);
            }
        }
        return null;
    }

    private JSONObject getMirrorResponse(Client client, String accountID) {

        final MirrorRestService restService = new MirrorRestService(getProperties(), client.getLedgerId());
        JSONObject jsonResponse = restService.get("accounts/" + accountID);
        if (jsonResponse == null) {
            LogUtil.warn(getClassName(), "Error retrieving data from mirror node.");
            return null;
        }
        return jsonResponse;
    }

    public String epochRenewPeriod(String epochTime) {
        try {
            if (!epochTime.contains(".")) {
                long epoch = Integer.valueOf(epochTime);

                Duration duration = Duration.ofSeconds(epoch);

                long days = duration.toDays();

                return days + " Days";
            }

        } catch (Exception e) {
            LogUtil.error(HederaAccountHashVariable.class.getName(), e,
                    "Epoch Time Error " + epochTime);
        }

        return null;
    }

    public String epochTimetoDate(String epochTime) {

        try {
            if (epochTime.contains(".")) {
                double epoch = Double.valueOf(epochTime);

                return new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm:ss a z")
                        .format(new java.util.Date((long) epoch * 1000)).toUpperCase();
            }

        } catch (Exception e) {
            LogUtil.error(HederaAccountHashVariable.class.getName(), e,
                    "Epoch Time Error " + epochTime);
        }

        return null;

    }

    @Override
    public String getPrefix() {
        return "hedera";
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getName() {
        return "Hedera Hash Variable";
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public Collection<String> availableSyntax() {
        Collection<String> syntax = new ArrayList<String>();
        syntax.add("hedera.account.allowances");
        syntax.add("hedera.account.allTokens");
        syntax.add("hedera.account.tokenBalance");
        syntax.add("hedera.account.hbarBalance");
        syntax.add("hedera.account.accountMemo");
        syntax.add("hedera.account.isAccountDeleted");
        syntax.add("hedera.account.receiverSignatureRequired");
        syntax.add("hedera.account.evmAddress");
        syntax.add("hedera.account.maxAutoTokenAssociations");
        syntax.add("hedera.account.alias");
        syntax.add("hedera.account.autoRenewPeriod");
        syntax.add("hedera.account.createdTimestamp");
        syntax.add("hedera.account.declineReward");
        syntax.add("hedera.account.ethereumNonce");
        syntax.add("hedera.account.expiryTimestamp");
        syntax.add("hedera.account.publicKeyType");
        syntax.add("hedera.account.publicKey");
        syntax.add("hedera.account.pendingReward");
        syntax.add("hedera.account.rewards");
        syntax.add("hedera.account.stakedAccountID");
        syntax.add("hedera.account.stakedNodeID");
        syntax.add("hedera.account.stakePeriodStart");

        return syntax;
    }

    @Override
    public String getPropertyAssistantDefinition() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        return AppUtil.readPluginResource(getClass().getName(), "/properties/HederaAccountHashVariable.json",
                new String[] { backendConfigs },
                true, PluginUtil.MESSAGE_PATH);
    }

}
