package org.joget.hedera.lib.hashvariable;

import com.hedera.hashgraph.sdk.Client;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaHashVariable;
import org.joget.hedera.service.MirrorRestService;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class HederaTokenHashVariable extends HederaHashVariable {
    
    @Override
    protected String processHashVariable(Client client, String variableKey) {
        if (!variableKey.contains("[") || !variableKey.contains("]")) {
            return null;
        }
        
        // Retrieve and remove Token ID from variableKey
        final String tokenId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
        
        final String attribute = variableKey.replace("[" + tokenId + "].", "");
        
        //If same account ID is already loaded on the existing context, read from cached request instead
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        final String tokenAttrKey = tokenId + "-tokenHashVar";
        
        JSONObject jsonResponse;
        if (request.getAttribute(tokenAttrKey) != null) {
            jsonResponse = (JSONObject) request.getAttribute(tokenAttrKey);
        } else {
            final MirrorRestService restService = new MirrorRestService(getProperties(), client.getLedgerId());
            jsonResponse = restService.getTokenData(tokenId);
            if (jsonResponse == null) {
                LogUtil.warn(getClassName(), "Error retrieving data from mirror node.");
                return null;
            }
            request.setAttribute(tokenAttrKey, jsonResponse);
        }
        
        try {
            return switch (attribute) {
                case "adminKey" -> 
                    !jsonResponse.isNull("admin_key")
                            ? String.valueOf(jsonResponse.getJSONObject("admin_key"))
                            : "none";
                case "autoRenewAccount" -> jsonResponse.getString("auto_renew_account");
                case "autoRenewPeriod" -> epochTimeToDays(jsonResponse.getInt("auto_renew_period"));
                case "createdTimestamp" -> epochTimeToDate(jsonResponse.getString("created_timestamp"));
                case "isDeleted" -> String.valueOf(jsonResponse.getBoolean("deleted"));
                case "decimals" -> jsonResponse.getString("decimals");
                case "expiryTimestamp" -> epochNanosecondsToDate(String.valueOf(jsonResponse.getLong("expiry_timestamp")));
                case "feeScheduleKey" -> 
                    !jsonResponse.isNull("fee_schedule_key")
                            ? String.valueOf(jsonResponse.getJSONObject("fee_schedule_key"))
                            : "none";
                case "isFreezeByDefault" -> String.valueOf(jsonResponse.getBoolean("freeze_default"));
                case "freezeKey" -> 
                    !jsonResponse.isNull("freeze_key")
                            ? String.valueOf(jsonResponse.getJSONObject("freeze_key"))
                            : "none";
                case "initialSupply" -> 
                    String.valueOf(
                            TransactionUtil.deriveTokenAmountBasedOnDecimals(
                                    Long.parseLong(jsonResponse.getString("initial_supply")), 
                                    Integer.parseInt(jsonResponse.getString("decimals"))
                            )
                    );
                case "kycKey" -> 
                    !jsonResponse.isNull("kyc_key")
                            ? String.valueOf(jsonResponse.getJSONObject("kyc_key"))
                            : "none";
                case "maxSupply" -> 
                    !(jsonResponse.getString("supply_type")).equalsIgnoreCase("INFINITE")
                            ? jsonResponse.getString("max_supply")
                            : "no limit";
                case "memo" -> jsonResponse.getString("memo");
                case "modifiedTimestamp" -> epochTimeToDate(jsonResponse.getString("modified_timestamp"));
                case "name" -> jsonResponse.getString("name");
                case "pauseKey" -> 
                    !jsonResponse.isNull("pause_key")
                            ? String.valueOf(jsonResponse.getJSONObject("pause_key"))
                            : "none";
                case "pauseStatus" -> jsonResponse.getString("pause_status");
                case "supplyKey" -> 
                    !jsonResponse.isNull("supply_key")
                            ? String.valueOf(jsonResponse.getJSONObject("supply_key"))
                            : "none";
                case "supplyType" -> jsonResponse.getString("supply_type");
                case "symbol" -> jsonResponse.getString("symbol");
                case "totalSupply" -> 
                    String.valueOf(
                            TransactionUtil.deriveTokenAmountBasedOnDecimals(
                                    Long.parseLong(jsonResponse.getString("total_supply")), 
                                    Integer.parseInt(jsonResponse.getString("decimals"))
                            )
                    );
                case "treasuryAccountId" -> jsonResponse.getString("treasury_account_id");
                case "type" -> jsonResponse.getString("type");
                case "wipeKey" -> 
                    !jsonResponse.isNull("wipe_key")
                            ? String.valueOf(jsonResponse.getJSONObject("wipe_key"))
                            : "none";
                case "customFees.createdTimestamp" -> epochTimeToDate(jsonResponse.getJSONObject("custom_fees").getString("created_timestamp"));
                case "customFees.fixedFees" -> String.valueOf(jsonResponse.getJSONObject("custom_fees").getJSONArray("fixed_fees"));
                case "customFees.fractionalFees" -> 
                    jsonResponse.getJSONObject("custom_fees").has("fractional_fees")
                            ? String.valueOf(jsonResponse.getJSONObject("custom_fees").getJSONArray("fractional_fees"))
                            : "not applicable";
                case "customFees.royaltyFees" -> 
                    jsonResponse.getJSONObject("custom_fees").has("royalty_fees")
                            ? String.valueOf(jsonResponse.getJSONObject("custom_fees").getJSONArray("royalty_fees"))
                            : "not applicable";
                default -> null;
            };
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error retrieving token attribute " + attribute);
        }
        
        return null;
    }
    
    private String epochTimeToDays(int epochTime) {
        try {
            return Duration.ofSeconds(epochTime).toDays() + " Days";
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Epoch Time Error " + epochTime);
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
    
    private String epochNanosecondsToDate(String epochTime) {
        try {
            return new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a z")
                    .format(new Date(TimeUnit.NANOSECONDS.toMillis(Long.parseLong(epochTime))))
                    .toUpperCase();
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Epoch Time Error " + epochTime);
        }

        return null;
    }
    
    @Override
    public String getPrefix() {
        return "hedera-token";
    }

    @Override
    public Collection<String> availableSyntax() {
        final String syntaxPrefix = getPrefix() + ".[TOKEN_ID].";
        
        Collection<String> syntax = new ArrayList<String>();
        syntax.add(syntaxPrefix + "adminKey");
        syntax.add(syntaxPrefix + "autoRenewAccount");
        syntax.add(syntaxPrefix + "autoRenewPeriod");
        syntax.add(syntaxPrefix + "createdTimestamp");
        syntax.add(syntaxPrefix + "isDeleted");
        syntax.add(syntaxPrefix + "decimals");
        syntax.add(syntaxPrefix + "expiryTimestamp");
        syntax.add(syntaxPrefix + "feeScheduleKey");
        syntax.add(syntaxPrefix + "isFreezeByDefault");
        syntax.add(syntaxPrefix + "freezeKey");
        syntax.add(syntaxPrefix + "initialSupply");
        syntax.add(syntaxPrefix + "kycKey");
        syntax.add(syntaxPrefix + "maxSupply");
        syntax.add(syntaxPrefix + "memo");
        syntax.add(syntaxPrefix + "modifiedTimestamp");
        syntax.add(syntaxPrefix + "name");
        syntax.add(syntaxPrefix + "pauseKey");
        syntax.add(syntaxPrefix + "pauseStatus");
        syntax.add(syntaxPrefix + "supplyKey");
        syntax.add(syntaxPrefix + "supplyType");
        syntax.add(syntaxPrefix + "symbol");
        syntax.add(syntaxPrefix + "totalSupply");
        syntax.add(syntaxPrefix + "treasuryAccountId");
        syntax.add(syntaxPrefix + "type");
        syntax.add(syntaxPrefix + "wipeKey");
        syntax.add(syntaxPrefix + "customFees.createdTimestamp");
        syntax.add(syntaxPrefix + "customFees.fixedFees");
        syntax.add(syntaxPrefix + "customFees.fractionalFees");
        syntax.add(syntaxPrefix + "customFees.royaltyFees");

        return syntax;
    }

    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClassName(), "/properties/hashvariable/HederaTokenHashVariable.json", null, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Load various values of fungible and non-fungible tokens from the Hedera DLT.";
    }

    @Override
    public String getName() {
        return "Hedera Token Hash Variable";
    }
}
