package com.noti.server.process.service.longterm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.noti.server.process.utils.IOUtils;
import com.noti.server.process.utils.Log;
import com.noti.server.process.packet.Device;

import java.io.*;

public class LongTermData {
    @JsonIgnore
    public static final String TAG = "LongTermData";

    @JsonProperty
    public Device originDevice;
    @JsonProperty
    public Device targetDevice;

    @JsonIgnore
    public String tempBuffer;
    @JsonProperty
    public long timestamp;

    @JsonProperty
    public String userId;
    @JsonProperty
    public String fileName;
    @JsonProperty
    public String parentDest;

    @SuppressWarnings("unused")
    public LongTermData() {
        // Default Constructor for Serializer
    }

    public LongTermData(String userId) {
        this.userId = userId;
    }

    public void read() throws IOException {
        File destFile = getFile();
        if(!isLoaded() | destFile.exists()) {
            tempBuffer = IOUtils.readFrom(destFile);
        } else {
            throw new IOException("Both of buffer and file are not-exists! abort read operation...");
        }
    }

    public void write() throws NullPointerException, IOException {
        if(!isLoaded()) {
            throw new NullPointerException("Buffer is blank, load data first!");
        }

        File destFile = getFile();
        if(!destFile.getParentFile().exists() & destFile.mkdirs()) {
            Log.printDebug(TAG, "User Folder Created: " + userId);
        }

        if(IOUtils.createNewFile(destFile)) {
            Log.printDebug(TAG, "File Created: " + destFile);
        }

        IOUtils.writeTo(destFile, tempBuffer);
    }

    public void deletePersist() {
        flushBuffer();
        File baseFile = getFile();
        if (baseFile.exists() & baseFile.delete()) {
            Log.printDebug(TAG, "Removed file with name: " + fileName);
        }
    }

    @JsonIgnore
    public boolean isLoaded() {
        return tempBuffer != null && !tempBuffer.isEmpty();
    }

    @JsonIgnore
    public boolean isExists() {
        return isLoaded() || getFile().exists();
    }

    @JsonIgnore
    public File getFile() {
        return new File(parentDest, String.format("/%s/%s", userId, fileName));
    }

    public void updateTimeStamp() {
        timestamp = System.currentTimeMillis();
    }

    public void flushBuffer() {
        tempBuffer = null;
    }
}