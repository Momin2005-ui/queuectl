package org.example.Helper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeHelper {

    public static String format(Instant instant) {
        if (instant == null) {
            return null;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

        return formatter.format(instant);
    }
}
