package org.joget.marketplace;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleInfo;
import com.hedera.hashgraph.sdk.ScheduleInfoQuery;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormLoadElementBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaScheduledTransactionLoadBinder extends FormBinder implements FormLoadBinder, FormLoadElementBinder {

    @Override
    public String getName() {
        return "Hedera Scheduled Transaction Load Binder";
    }

    @Override
    public String getVersion() {
        return "7.0.0";
    }

    @Override
    public String getDescription() {
        return "Load data of a scheduled transaction from the Hedera DLT into a form.";
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final String scheduleId = WorkflowUtil.processVariable(getPropertyString("scheduleId"), "", null);

        //Prevent error thrown from empty value and invalid hash variable
        if (scheduleId.isEmpty() || scheduleId.startsWith("#")) {
            return null;
        }
        
        final String operatorId = getPropertyString("operatorId");
        final String operatorKey = getPropertyString("operatorKey");
        final String networkType = getPropertyString("networkType");
        
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        FormRowSet rows = new FormRowSet();
        
        try {
            final Client client = HederaUtil.getHederaClient(operatorId, operatorKey, networkType);
            
            if (client != null) {
                ScheduleId scheduleIdObj = ScheduleId.fromString(scheduleId);
                
                ScheduleInfo scheduleInfo = new ScheduleInfoQuery()
                    .setScheduleId(scheduleIdObj)
                    .execute(client);
                
                /* Get form fields from plugin properties */
                String scheduledTransactionIdField = getPropertyString("scheduledTransactionIdField");
                String transactionSignatoriesField = getPropertyString("transactionSignatoriesField");
                String transactionMemoField = getPropertyString("transactionMemoField");
                String creatorAccountIdField = getPropertyString("creatorAccountIdField");
                String payerAccountIdField = getPropertyString("payerAccountIdField");
                String expirationTimeField = getPropertyString("expirationTimeField");
                String executedAtField = getPropertyString("executedAtField");
                String deletedAtField = getPropertyString("deletedAtField");
                
                FormRow row = new FormRow();
                
                //The transaction ID of this scheduled transaction
                row = addRow(row, scheduledTransactionIdField, scheduleInfo.scheduledTransactionId.toString());
                //Who has signed this transaction so far
                row = addRow(row, transactionSignatoriesField, scheduleInfo.signatories.toString());
                //The transaction memo
                row = addRow(row, transactionMemoField, scheduleInfo.memo);
                //The Hedera account that created this scheduled transaction
                row = addRow(row, creatorAccountIdField, scheduleInfo.creatorAccountId.toString());
                //The Hedera account paying for the execution of this scheduled transaction
                row = addRow(row, payerAccountIdField, scheduleInfo.payerAccountId.toString());
                //The date and time when this scheduled transaction will expire
                row = addRow(row, expirationTimeField, HederaUtil.convertInstantToZonedDateTimeString(scheduleInfo.expirationTime));
                //The time the schedule transaction was executed. If the schedule transaction has not executed this field will be left null.
                row = addRow(row, executedAtField, HederaUtil.convertInstantToZonedDateTimeString(scheduleInfo.executedAt));
                //The consensus time the schedule transaction was deleted. If the schedule transaction was not deleted, this field will be left null.
                row = addRow(row, deletedAtField, HederaUtil.convertInstantToZonedDateTimeString(scheduleInfo.deletedAt));
                
                rows.add(row);
            }
            
            return rows;
        } catch (Exception ex) {
            LogUtil.error(getClass().getName(), ex, "Error executing plugin...");
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        return row;
    }
    
    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/HederaScheduledTransactionLoadBinder.json", null, true, "messages/HederaMessages");
    }
}
