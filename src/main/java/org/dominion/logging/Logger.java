package org.dominion.logging;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static void log(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String timestamp = "[" + sdf.format(new Date()) + "] ";

        System.out.println(timestamp + message + ConsoleColors.RESET);
    }

}
