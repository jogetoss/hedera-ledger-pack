package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.Mnemonic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicDeleteTransaction;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TopicUpdateTransaction;
import com.hedera.hashgraph.sdk.TransactionRecord;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.bouncycastle.util.encoders.Hex;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.HederaProcessTool;
import org.joget.hedera.service.AccountUtil;
import org.joget.hedera.service.PluginUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaTopicManagementTool extends HederaProcessTool {
    
    @Override
    public String getName() {
        return "Hedera Topic Management Tool";
    }

    @Override
    public String getDescription() {
        return "Performs various topic-related functions offered by the Hedera Consensus Service.";
    }

    @Override
    public String getPropertyOptions() {
        String backendConfigs = PluginUtil.readGenericBackendConfigs(getClassName());
        String wfVarMappings = PluginUtil.readGenericWorkflowVariableMappings(getClassName());
        return AppUtil.readPluginResource(getClassName(), "/properties/HederaTopicManagementTool.json", new String[]{backendConfigs, wfVarMappings}, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    protected Object runTool(Map props) 
            throws TimeoutException, RuntimeException {
        
        try {
            TransactionRecord transactionRecord;
            
            switch (OperationType.fromString(getPropertyString("operationType"))) {
                case SUBMIT_MESSAGE: {
                    final TopicId topicId = TopicId.fromString(getPropertyString("topicId"));
                    final String message = getPropertyString("message");
                    TopicMessageSubmitTransaction topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                            .setTopicId(topicId)
                            .setMessage(message)
                            .freezeWith(client);
                    
                    if ("true".equalsIgnoreCase(getPropertyString("requireSubmitKey"))) {
                        topicMessageSubmitTransaction.sign(
                                PrivateKey.fromStringDER(PluginUtil.decrypt(
                                        WorkflowUtil.processVariable(getPropertyString("submitKey"), "", wfAssignment)
                                ))
                        );
                    }
                    
                    transactionRecord = topicMessageSubmitTransaction
                            .execute(client)
                            .getRecord(client);
                    
                    storeValuetoActivityVar(
                            wfAssignment.getActivityId(), 
                            getPropertyString("wfTopicCurrentSequence"), 
                            transactionRecord.receipt.topicSequenceNumber.toString()
                    );
                    storeValuetoActivityVar(
                            wfAssignment.getActivityId(), 
                            getPropertyString("wfTopicCurrentHash"), 
                            new String(Hex.encode(transactionRecord.receipt.topicRunningHash.toByteArray()))
                    );
                    break;
                }
                case CREATE_TOPIC: {
                    TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();
                    
                    switch (getPropertyString("createTopicAdminKey")) {
                        case "operator": {
                            topicCreateTransaction.setAdminKey(client.getOperatorPublicKey());
                            break;
                        }
                        case "custom": {
                            topicCreateTransaction.setAdminKey(getPublicKey(getPropertyString("adminAccountMnemonic")));
                            break;
                        }
                    }
                    
                    String encryptedSubmitKey = null;
                    if ("true".equalsIgnoreCase(getPropertyString("setAsPrivateTopic"))) {
                        PrivateKey newSubmitKey = PrivateKey.generateED25519();
                        topicCreateTransaction.setSubmitKey(newSubmitKey);
                        encryptedSubmitKey = PluginUtil.encrypt(newSubmitKey.toStringDER());
                    }
                    
                    final String topicMemo = WorkflowUtil.processVariable(getPropertyString("topicMemo"), "", wfAssignment).trim();
                    if (!topicMemo.isBlank()) {
                        topicCreateTransaction.setTopicMemo(topicMemo);
                    }
                    
                    topicCreateTransaction = topicCreateTransaction.freezeWith(client);
                    
                    if ("custom".equalsIgnoreCase(getPropertyString("adminKey"))) {
                        topicCreateTransaction = topicCreateTransaction.sign(getPrivateKey(getPropertyString("adminAccountMnemonic")));
                    }
                            
                    transactionRecord = topicCreateTransaction
                            .execute(client)
                            .getRecord(client);
                    
                    storeValuetoActivityVar(
                            wfAssignment.getActivityId(), 
                            getPropertyString("wfTopicId"), 
                            transactionRecord.receipt.topicId.toString()
                    );
                    storeValuetoActivityVar(
                            wfAssignment.getActivityId(), 
                            getPropertyString("wfSubmitKey"), 
                            encryptedSubmitKey
                    );
                    break;
                }
                case UPDATE_TOPIC: {
                    final TopicId topicId = TopicId.fromString(getPropertyString("topicId"));
                    TopicUpdateTransaction topicUpdateTransaction = new TopicUpdateTransaction().setTopicId(topicId);
                    
                    switch (getPropertyString("modifyAdminKey")) {
                        case "remove": {
                            topicUpdateTransaction.clearAdminKey();
                            break;
                        }
                        case "replaceToOperator": {
                            topicUpdateTransaction.setAdminKey(client.getOperatorPublicKey());
                            break;
                        }
                        case "replaceToCustom": {
                            topicUpdateTransaction.setAdminKey(getPublicKey(getPropertyString("newAdminAccountMnemonic")));
                            break;
                        }
                    }
                    
                    String encryptedSubmitKey = null;
                    switch (getPropertyString("modifySubmitKey")) {
                        case "remove": {
                            topicUpdateTransaction.clearSubmitKey();
                            encryptedSubmitKey = "";
                            break;
                        }
                        case "replace": {
                            PrivateKey newSubmitKey = PrivateKey.generateED25519();
                            topicUpdateTransaction.setSubmitKey(newSubmitKey);
                            encryptedSubmitKey = PluginUtil.encrypt(newSubmitKey.toStringDER());
                            break;
                        }
                    }
                    
                    switch (getPropertyString("modifyTopicMemo")) {
                        case "remove": {
                            topicUpdateTransaction.clearTopicMemo();
                            break;
                        }
                        case "replace": {
                            final String newTopicMemo = WorkflowUtil.processVariable(getPropertyString("newTopicMemo"), "", wfAssignment).trim();
                            topicUpdateTransaction.setTopicMemo(newTopicMemo);
                            break;
                        }
                    }
                    
                    topicUpdateTransaction.freezeWith(client);
                    
                    if ("custom".equalsIgnoreCase(getPropertyString("updateTopicAdminKey"))) {
                        topicUpdateTransaction = topicUpdateTransaction.sign(getPrivateKey(getPropertyString("adminAccountMnemonic")));
                    }
                    
                    if ("replaceToCustom".equalsIgnoreCase(getPropertyString("modifyAdminKey"))) {
                        topicUpdateTransaction = topicUpdateTransaction.sign(getPrivateKey(getPropertyString("newAdminAccountMnemonic")));
                    }
                    
                    transactionRecord = topicUpdateTransaction
                            .execute(client)
                            .getRecord(client);
                    
                    storeValuetoActivityVar(
                            wfAssignment.getActivityId(), 
                            getPropertyString("wfReplacedSubmitKey"), 
                            encryptedSubmitKey
                    );
                    break;
                }
                case DELETE_TOPIC: {
                    final TopicId topicId = TopicId.fromString(getPropertyString("topicId"));
                    TopicDeleteTransaction topicDeleteTransaction = new TopicDeleteTransaction()
                            .setTopicId(topicId)
                            .freezeWith(client);
                    
                    if ("custom".equalsIgnoreCase(getPropertyString("deleteTopicAdminKey"))) {
                        topicDeleteTransaction = topicDeleteTransaction.sign(getPrivateKey(getPropertyString("adminAccountMnemonic")));
                    }
                    
                    transactionRecord = topicDeleteTransaction
                            .execute(client)
                            .getRecord(client);
                    break;
                }
                default:
                    LogUtil.warn(getClassName(), "Unknown topic management operation type!");
                    return null;
            }
            
            storeGenericTxDataToWorkflowVariable(transactionRecord);
            
            return transactionRecord;
        } catch (PrecheckStatusException | ReceiptStatusException e) {
            throw new RuntimeException(e.getClass().getName() + " : " + e.getMessage());
        }
    }
    
    
    
    private PublicKey getPublicKey(String mnemonicString) {
        try {
            return AccountUtil.derivePublicKeyFromMnemonic(
                    Mnemonic.fromString(
                            PluginUtil.decrypt(
                                    WorkflowUtil.processVariable(mnemonicString, "", wfAssignment)
                            )
                    )
            );
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Unable to derive private key from mnemonic...");
            return null;
        }
    }
    
    private PrivateKey getPrivateKey(String mnemonicString) {
        try {
            return AccountUtil.derivePrivateKeyFromMnemonic(
                    Mnemonic.fromString(
                            PluginUtil.decrypt(
                                    WorkflowUtil.processVariable(mnemonicString, "", wfAssignment)
                            )
                    )
            );
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Unable to derive private key from mnemonic...");
            return null;
        }
    }
    
    private enum OperationType {
        SUBMIT_MESSAGE("submitMessage"),
        CREATE_TOPIC("createTopic"),
        UPDATE_TOPIC("updateTopic"),
        DELETE_TOPIC("deleteTopic");
        
        private final String value;
        
        OperationType(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static OperationType fromString(String text) {
            for (OperationType operation : OperationType.values()) {
                if ((operation.value).equalsIgnoreCase(text)) {
                    return operation;
                }
            }

            return null;
        }
    }
}
