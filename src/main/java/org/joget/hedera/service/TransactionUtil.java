package org.joget.hedera.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionUtil {
    
    public static int calcActualTokenAmountBasedOnDecimals(String precalcAmount, int decimalPoints) {
        BigDecimal unscaled = new BigDecimal(precalcAmount);
        BigDecimal scaled = unscaled.scaleByPowerOfTen(decimalPoints);
        
        //If "token amount" exceeds the configured token decimals, the exceeded numbers are ignored.
        return scaled.intValue();
    }
    
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
