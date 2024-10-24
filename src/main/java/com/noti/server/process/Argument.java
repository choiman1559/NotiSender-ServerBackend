package com.noti.server.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noti.server.process.service.longterm.LongTermArgument;
import com.noti.server.process.service.shorterm.ShortTermArgument;

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

    public boolean useAuthentication;
    public boolean allowBlankAuthHeader;
    public String authCredentialPath;

    public String liveNotiProperties;
    public String imageCacheProperties;
    public String fileListProperties;

    public ShortTermArgument liveNotiArgument;
    public ShortTermArgument imageCacheArgument;
    public LongTermArgument fileListArgument;

    public static Argument buildFrom(List<String> argument) throws IOException, IllegalArgumentException {
        if(argument.isEmpty()) {
            throw new IllegalArgumentException("argument is not found!");
        }

        File file = new File(argument.get(0));
        if(file.exists() && file.canRead()) {
            Argument argumentObj = (Argument) parsePropertiesFromFile(file.getPath(), Argument.class);
            argumentObj.liveNotiArgument = (ShortTermArgument) parsePropertiesFromFile(argumentObj.liveNotiProperties, ShortTermArgument.class);
            argumentObj.imageCacheArgument = (ShortTermArgument) parsePropertiesFromFile(argumentObj.imageCacheProperties, ShortTermArgument.class);
            argumentObj.fileListArgument = (LongTermArgument) parsePropertiesFromFile(argumentObj.fileListProperties, LongTermArgument.class);

            return argumentObj;
        } else {
            throw new FileNotFoundException("com.noti.server.process.Argument File not found or Not Accessible");
        }
    }

    private static Object parsePropertiesFromFile(String filePath, Class<?> cls) throws IOException {
        Properties fileProps = new Properties();
        fileProps.load(new FileInputStream(filePath));

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(fileProps, cls);
    }
}
