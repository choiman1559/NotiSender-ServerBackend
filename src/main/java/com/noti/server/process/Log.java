package com.noti.server.process;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    public static void print(String tag, String message) {
        String date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").format(new Date(System.currentTimeMillis()));
        System.out.printf("%s [%s] %s%n", date, tag, message);
    }
}
