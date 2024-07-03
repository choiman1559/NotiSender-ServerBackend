package com.noti.server.process;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class Argument {
    public int port;
    public boolean isDebug;
    public String host;
    public String matchVersion;

    public long liveNotificationDatabaseGCInterval;
    public long liveNotificationDatabaseObjLifeTime;

    public static Argument buildFrom(List<String> argument) throws IOException, IllegalArgumentException {
        if(argument.isEmpty()) {
            throw new IllegalArgumentException("argument is not found!");
        }

        File file = new File(argument.get(0));
        if(file.exists() && file.canRead()) {
            Properties fileProps = new Properties();
            fileProps.load(new FileInputStream(file));

            final ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(fileProps, Argument.class);
        } else {
            throw new FileNotFoundException("com.noti.server.process.Argument File not found or Not Accessible");
        }
    }
}
