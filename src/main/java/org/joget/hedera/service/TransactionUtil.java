package org.joget.hedera.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionUtil {
    
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
