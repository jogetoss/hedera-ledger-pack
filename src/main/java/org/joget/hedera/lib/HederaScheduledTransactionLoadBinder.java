package org.joget.hedera.lib;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleInfo;
import com.hedera.hashgraph.sdk.ScheduleInfoQuery;
import java.util.concurrent.TimeoutException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadElementBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.hedera.model.HederaFormBinderAbstract;
import org.joget.hedera.service.PluginUtil;
import org.joget.hedera.service.TransactionUtil;
import org.joget.workflow.util.WorkflowUtil;

public class HederaScheduledTransactionLoadBinder extends HederaFormBinderAbstract implements FormLoadElementBinder {

    @Override
    public String getName() {
        return "Hedera Scheduled Transaction Load Binder";
    }

    @Override
    public String getDescription() {
        return "Load data of a scheduled transaction from the Hedera DLT into a form.";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/HederaScheduledTransactionLoadBinder.json", null, true, PluginUtil.MESSAGE_PATH);
    }
    
    @Override
    public FormRowSet loadData(Client client, Element element, String primaryKey, FormData formData) 
            throws TimeoutException, PrecheckStatusException {
        
        final String scheduleId = WorkflowUtil.processVariable(getPropertyString("scheduleId"), "", null);

        //Prevent error thrown from empty value and invalid hash variable
        if (scheduleId.isEmpty() || scheduleId.startsWith("#")) {
            return null;
        }

        ScheduleInfo scheduleInfo = new ScheduleInfoQuery()
            .setScheduleId(ScheduleId.fromString(scheduleId))
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
        row = addRow(row, expirationTimeField, TransactionUtil.convertInstantToZonedDateTimeString(scheduleInfo.expirationTime));
        //The time the schedule transaction was executed. If the schedule transaction has not executed this field will be left null.
        row = addRow(row, executedAtField, TransactionUtil.convertInstantToZonedDateTimeString(scheduleInfo.executedAt));
        //The consensus time the schedule transaction was deleted. If the schedule transaction was not deleted, this field will be left null.
        row = addRow(row, deletedAtField, TransactionUtil.convertInstantToZonedDateTimeString(scheduleInfo.deletedAt));

        FormRowSet rows = new FormRowSet();
        rows.add(row);
        
        return rows;
    }
    
    private FormRow addRow(FormRow row, String field, String value) {
        if (row != null && !field.isEmpty()) {
            row.put(field, value);
        }
        
        return row;
    }
}
