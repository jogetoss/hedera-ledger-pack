package org.joget.hedera.service;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.TransactionRecordQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.joget.apps.form.service.FormUtil;

public class TransactionUtil {
    
    private TransactionUtil() {}
    
    public static boolean isTransactionExist(Client client, String transactionId) {
        //If within a Form Builder, don't make useless API calls
        if (transactionId == null || transactionId.isBlank() || FormUtil.isFormBuilderActive()) {
            return false;
        }
        
        try {
            TransactionRecord txRecord = new TransactionRecordQuery()
                    .setTransactionId(TransactionId.fromString(transactionId))
                    .execute(client);
            
            return (txRecord != null);
        } catch (Exception ex) {
            //Ignore if not successful.
        }
        
        return false;
    }
    
    /**
     * Convert human-readable amount to actual number (e.g.: 2 decimal points, 500.00 --> 50000)
     */
    public static int calcActualTokenAmountBasedOnDecimals(String precalcAmount, int decimalPoints) {
        BigDecimal unscaled = new BigDecimal(precalcAmount);
        BigDecimal scaled = unscaled.scaleByPowerOfTen(decimalPoints);
        
        //If "token amount" exceeds the configured token decimals, the exceeded numbers are ignored.
        return scaled.intValue();
    }
    
    /**
     * Convert actual amount to human-readable format (e.g.: 2 decimal points, 10000 --> 100.00)
     */ 
    public static BigDecimal deriveTokenAmountBasedOnDecimals(long actualAmount, int decimalPoints) {
        BigDecimal unscaled = new BigDecimal(actualAmount);
        BigDecimal scaled = unscaled.scaleByPowerOfTen(-decimalPoints);
        
        return scaled;
    }
    
    public static String convertInstantToZonedDateTimeString(Instant timeStamp) {
        return convertInstantToZonedDateTimeString(timeStamp, null);
    }
    
    public static String convertInstantToZonedDateTimeString(Instant timeStamp, String zoneId) {
        if (timeStamp == null) {
            return "";
        }
        
        String dateTimeFormat = "yyyy-MM-dd hh:mm:ss a z";
        
        //Default to UTC timezone
        ZoneId zone;
        if (zoneId != null && !zoneId.isEmpty()) {
            zone = ZoneId.of(zoneId);
        } else {
            zone = ZoneId.of("UTC");
        }
        
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(timeStamp, zone);
        
        return DateTimeFormatter.ofPattern(dateTimeFormat).format(dateTime);
    }
}
