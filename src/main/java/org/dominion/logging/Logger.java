package org.dominion.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static void log(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String timestamp = "[" + sdf.format(new Date()) + "] ";

        System.out.println(timestamp + message + ConsoleColors.RESET);
    }

}
