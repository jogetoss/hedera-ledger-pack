package org.joget.hedera.lib;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Process Tool plugins
        registrationList.add(context.registerService(HederaGenerateAccountTool.class.getName(), new HederaGenerateAccountTool(), null));
        registrationList.add(context.registerService(HederaSendTransactionTool.class.getName(), new HederaSendTransactionTool(), null));
        registrationList.add(context.registerService(HederaSignScheduledTransactionTool.class.getName(), new HederaSignScheduledTransactionTool(), null));
    
        //Form Binder plugins
        registrationList.add(context.registerService(HederaAccountLoadBinder.class.getName(), new HederaAccountLoadBinder(), null));
        registrationList.add(context.registerService(HederaScheduledTransactionLoadBinder.class.getName(), new HederaScheduledTransactionLoadBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}