package org.joget.hedera.model.explorer;

import org.joget.commons.util.LogUtil;
import com.hedera.hashgraph.sdk.LedgerId;

public class ExplorerFactory {
    
    public static final String DEFAULT_EXPLORER = "hashscan";
    
    private final LedgerId ledgerId;

    public ExplorerFactory(LedgerId ledgerId) {
        this.ledgerId = ledgerId;
    }
    
    public Explorer createExplorer(String explorerType) {
        switch (explorerType) {
            case DEFAULT_EXPLORER:
                return new HashScan(ledgerId);
            case "dragonglass" :
                return new DragonGlass(ledgerId);
            default:
                LogUtil.warn(getClassName(), "Unknown explorer type found!");
                return null;
        }
    }
    
    private static String getClassName() {
        return ExplorerFactory.class.getName();
    }
}
