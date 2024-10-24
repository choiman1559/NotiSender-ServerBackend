package com.noti.server.process.service.longterm;

import com.noti.server.process.Log;
import com.noti.server.process.packet.Device;
import kotlinx.io.Buffer;
import kotlinx.io.BuffersJvmKt;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LongTermData {
    public static final String TAG = "LongTermData";

    Device originDevice;
    Device targetDevice;

    String userName;
    String tempBuffer;
    long timestamp;

    String fileName;
    String parentDest;

    public void read() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if(isLoaded() && getFile().exists()) {
            try (Buffer fileBuffer = new Buffer(); FileInputStream fileInputStream = new FileInputStream(getFile())) {
                BuffersJvmKt.readTo(BuffersJvmKt.transferFrom(fileBuffer, fileInputStream), outputStream, getFile().length());
                tempBuffer = outputStream.toString();
            }
        }
    }

    public void write() throws NullPointerException, IOException {
        if(!isLoaded()) {
            throw new NullPointerException("Buffer is blank, load data first!");
        }

        if(!getFile().exists()) {
            getFile().createNewFile();
        }

        byte[] dataArray = tempBuffer.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dataArray);
        try (Buffer fileBuffer = new Buffer(); FileOutputStream fileOutputStream = new FileOutputStream(getFile())) {
            BuffersJvmKt.readTo(BuffersJvmKt.transferFrom(fileBuffer, inputStream), fileOutputStream, dataArray.length);
        }
    }

    public void deletePersist() {
        flushBuffer();
        File baseFile = getFile();
        if (baseFile.exists() & baseFile.delete()) {
            Log.print(TAG, "Removed file with name: " + fileName);
        }
    }

    public boolean isLoaded() {
        return tempBuffer != null && !tempBuffer.isEmpty();
    }

    public File getFile() {
        return new File(parentDest, fileName);
    }

    public void updateTimeStamp() {
        timestamp = System.currentTimeMillis();
    }

    public void flushBuffer() {
        tempBuffer = null;
    }
}