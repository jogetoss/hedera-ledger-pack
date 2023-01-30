package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.BadMnemonicException;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.NftId;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.ScheduleCreateTransaction;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.hedera.model.HederaProcessToolAbstract;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;

public class HederaSendTransactionTool extends HederaProcessToolAbstract {
    
    @Override
    public String getName() {
        return "Hedera Send Transaction Tool";
    }

    @Override
    public String getDescription() {
        return "Send tokens from one account to another on the Hedera DLT, with option to schedule transactions.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaSendTransactionTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public boolean isInputDataValid(Map props) {
        final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
        
        try {
            Mnemonic.fromString(accountMnemonic);
        } catch (BadMnemonicException ex) {
            LogUtil.warn(getClassName(), "Send transaction aborted. Sender account encountered bad/invalid mnemonic phrase.");
            return false;
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Send transaction aborted. Unexpected error when attempting to validate mnemonic phrase.");
            return false;
        }
        
        String formDefId = getPropertyString("formDefId");
        final String primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        FormRowSet rowSet = getFormRecord(formDefId, null);
        
        if (rowSet == null || rowSet.isEmpty()) {
            LogUtil.warn(getClassName(), "Send transaction aborted. No record found with record ID '" + primaryKey + "' from this form '" + formDefId + "'");
            return false;
        }
        
        return true;
    }
    
    @Override
    protected Object runTool(Map props, Client client) 
            throws TimeoutException, RuntimeException {
        
        try {
            String formDefId = getPropertyString("formDefId");

            FormRowSet rowSet = getFormRecord(formDefId, null);

            FormRow row = rowSet.get(0);

            final String paymentUnit = getPropertyString("paymentUnit");
            final String senderAccountId = row.getProperty(getPropertyString("senderAccountId"));
            final String accountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("accountMnemonic"), "", wfAssignment));
            final String receiverAccountId = row.getProperty(getPropertyString("receiverAccountId"));
            final String amount = row.getProperty(getPropertyString("amount"));
            final String transactionMemo = WorkflowUtil.processVariable(getPropertyString("transactionMemo"), "", wfAssignment);
            final boolean enableScheduledTx = "true".equals(getPropertyString("enableScheduledTransaction"));

            final AccountId senderAccount = AccountId.fromString(senderAccountId);
            final PrivateKey senderPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(accountMnemonic));

            final AccountId receiverAccount = AccountId.fromString(receiverAccountId);

            TransferTransaction transferTransaction = new TransferTransaction();

            if ("nft".equals(paymentUnit) || "nativeTokens".equals(paymentUnit)) {
                final String tokenId = WorkflowUtil.processVariable(getPropertyString("tokenId"), "", wfAssignment);

                //Auto-associate tokens with receiver here
                final boolean enableTokenAutoAssociateWithReceiver = "true".equals(getPropertyString("enableTokenAutoAssociateWithReceiver"));
                if (enableTokenAutoAssociateWithReceiver) {
                    //IDEA: If don't want to require mnemonic, perhaps check for receiver's automatic token associations + number of tokens already existed
                    final String receiverAccountMnemonic = PluginUtil.decrypt(WorkflowUtil.processVariable(getPropertyString("receiverAccountMnemonic"), "", wfAssignment));
                    final PrivateKey receiverPrivateKey = AccountUtil.derivePrivateKeyFromMnemonic(Mnemonic.fromString(receiverAccountMnemonic));

                    //Check if token already associated before doing so
                    AccountBalance accountBalances = new AccountBalanceQuery()
                            .setAccountId(receiverAccount)
                            .execute(client);

                    if (accountBalances.tokens.get(TokenId.fromString(tokenId)) == null) {
                        new TokenAssociateTransaction()
                            .setAccountId(receiverAccount)
                            .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId)))
                            .freezeWith(client)
                            .sign(receiverPrivateKey)
                            .execute(client)
                            .getReceipt(client);
                    }
                }

                switch (paymentUnit) {
                    case "nft": {
                        final String nftSerialNumber = WorkflowUtil.processVariable(getPropertyString("nftSerialNumber"), "", wfAssignment);

                        NftId nftId = new NftId(TokenId.fromString(tokenId), Long.parseLong(nftSerialNumber));

                        transferTransaction
                                .addNftTransfer(nftId, senderAccount, receiverAccount);
                        break;
                    }
                    case "nativeTokens": {
                        //Auto-calc token decimals for transfer amount
                        TokenInfo tokenInfo = new TokenInfoQuery()
                            .setTokenId(TokenId.fromString(tokenId))
                            .execute(client);

                        int actualAmount = TransactionUtil.calcActualTokenAmountBasedOnDecimals(amount, tokenInfo.decimals);

                        transferTransaction
                                .addTokenTransfer(TokenId.fromString(tokenId), senderAccount, Math.negateExact(actualAmount))
                                .addTokenTransfer(TokenId.fromString(tokenId), receiverAccount, actualAmount);
                        break;
                    }
                }
            } else {
                //1 hbar = 100,000,000 tinybars
                Hbar amountHbar = Hbar.fromString(amount);

                transferTransaction
                        .addHbarTransfer(senderAccount, amountHbar.negated())
                        .addHbarTransfer(receiverAccount, amountHbar);
            }

            //Can set a transaction memo of string up to max length of 100
            if (!transactionMemo.isBlank()) {
                transferTransaction.setTransactionMemo(transactionMemo);
            }

            TransactionRecord transactionRecord;

            if (enableScheduledTx) {
                transactionRecord = new ScheduleCreateTransaction()
                    .setScheduledTransaction(transferTransaction)
                    .setAdminKey(client.getOperatorPublicKey())
                    .setPayerAccountId(senderAccount)
                    .freezeWith(client)
                    .sign(senderPrivateKey)
                    .execute(client)
                    .getRecord(client);
            } else {
                transactionRecord = transferTransaction
                    .freezeWith(client)
                    .sign(senderPrivateKey)
                    .execute(client)
                    .getRecord(client);
            }

            storeGenericTxDataToWorkflowVariable(props, transactionRecord);
            storeAdditionalDataToWorkflowVariable(props, transactionRecord);

            return transactionRecord;
        } catch (PrecheckStatusException | BadMnemonicException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    protected void storeAdditionalDataToWorkflowVariable(Map properties, TransactionRecord transactionRecord) {
        String wfScheduleId = getPropertyString("wfScheduleId");
        
        storeValuetoActivityVar(
                wfAssignment.getActivityId(), 
                wfScheduleId, 
                transactionRecord.receipt.scheduleId != null ? transactionRecord.receipt.scheduleId.toString() : ""
        );
    }
}
