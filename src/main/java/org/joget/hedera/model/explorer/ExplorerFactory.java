package org.joget.hedera.model.explorer;

import org.joget.commons.util.LogUtil;
import org.joget.hedera.model.NetworkType;

public class ExplorerFactory {
    
    public static final String DEFAULT_EXPLORER = "hashscan";
    
    private final NetworkType networkType;

    public ExplorerFactory(NetworkType networkType) {
        this.networkType = networkType;
    }
    
    public Explorer createExplorer(String explorerType) {
        switch (explorerType) {
            case DEFAULT_EXPLORER:
                return new HashScan(networkType);
            case "dragonglass" :
                return new DragonGlass(networkType);
            default:
                LogUtil.warn(getClassName(), "Unknown explorer type found!");
                return null;
        }
    }
    
    private static String getClassName() {
        return ExplorerFactory.class.getName();
    }
}
