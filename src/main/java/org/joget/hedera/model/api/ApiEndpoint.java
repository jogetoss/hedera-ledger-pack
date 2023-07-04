package org.joget.hedera.model.api;

import com.hedera.hashgraph.sdk.LedgerId;

public interface ApiEndpoint {
    String getEndpoint(LedgerId ledgerId);
}
