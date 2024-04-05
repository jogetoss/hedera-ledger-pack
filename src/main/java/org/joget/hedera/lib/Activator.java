package org.joget.hedera.lib;

import java.util.ArrayList;
import java.util.Collection;
import org.joget.hedera.lib.plugindefaultproperties.*;
import org.joget.hedera.lib.hashvariable.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Default Properties plugins
        registrationList.add(context.registerService(HederaDefaultBackendConfigurator.class.getName(), new HederaDefaultBackendConfigurator(), null));
        
        //Process Tool plugins
        registrationList.add(context.registerService(HederaGenerateAccountTool.class.getName(), new HederaGenerateAccountTool(), null));
        registrationList.add(context.registerService(HederaSendTransactionTool.class.getName(), new HederaSendTransactionTool(), null));
        registrationList.add(context.registerService(HederaSignScheduledTransactionTool.class.getName(), new HederaSignScheduledTransactionTool(), null));
        registrationList.add(context.registerService(HederaMintTokenTool.class.getName(), new HederaMintTokenTool(), null));
        registrationList.add(context.registerService(HederaTokenManagementTool.class.getName(), new HederaTokenManagementTool(), null));
        registrationList.add(context.registerService(HederaTopicManagementTool.class.getName(), new HederaTopicManagementTool(), null));
    
        //Form Binder plugins
        registrationList.add(context.registerService(HederaAccountLoadBinder.class.getName(), new HederaAccountLoadBinder(), null));
        registrationList.add(context.registerService(HederaScheduledTransactionLoadBinder.class.getName(), new HederaScheduledTransactionLoadBinder(), null));
        
        //Form Element plugins
        registrationList.add(context.registerService(HederaExplorerLinkFormElement.class.getName(), new HederaExplorerLinkFormElement(), null));

        //Hash Variable plugins
        registrationList.add(context.registerService(HederaAccountHashVariable.class.getName(), new HederaAccountHashVariable(), null));
        registrationList.add(context.registerService(HederaTopicHashVariable.class.getName(), new HederaTopicHashVariable(), null));
        registrationList.add(context.registerService(HederaTransactionHashVariable.class.getName(), new HederaTransactionHashVariable(), null));
        registrationList.add(context.registerService(HederaTokenHashVariable.class.getName(), new HederaTokenHashVariable(), null));
        registrationList.add(context.registerService(HederaScheduleHashVariable.class.getName(), new HederaScheduleHashVariable(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}
