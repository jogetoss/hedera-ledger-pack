package org.joget.hedera.model.explorer;

public interface Explorer {
    String getTransactionUrl(String transactionId);
    String getAccountUrl(String accountId);
    String getTokenUrl(String tokenId);
    String getTopicUrl(String topicId);     
}
