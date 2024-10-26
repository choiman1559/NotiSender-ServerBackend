package com.noti.server.process.service.longterm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noti.server.module.EchoApp;
import com.noti.server.process.Argument;
import com.noti.server.process.utils.IOUtils;
import com.noti.server.process.utils.Log;
import com.noti.server.process.Service;
import com.noti.server.process.service.model.ProcessModel;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.*;

public class LongTermProcess implements ProcessModel {

    protected final String BACKUP_FOLDER = "/.config";
    protected final String BACKUP_DATA_MAP = "/longTermDataMap.json";
    protected final String BACKUP_DATA_STACK = "/recentLongTermDataStack.json";

    private final String LOG_TAG;
    public final LongTermArgument longTermArgument;
    public volatile ConcurrentHashMap<String, ConcurrentHashMap<String, LongTermData>> longTermDataMap;
    public volatile CopyOnWriteArrayList<String[]> recentLongTermDataStack = new CopyOnWriteArrayList<>();
    private final Thread longTermDataTimeoutWatchThread = new Thread(this::performLiveObjGC);

    public LongTermProcess(String LOG_TAG, LongTermArgument longTermArgument) {
        this.LOG_TAG = LOG_TAG;
        this.longTermArgument = longTermArgument;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void startTimeoutWatchThread() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            CopyOnWriteArrayList<ArrayList<String>> tempArrayList = (CopyOnWriteArrayList<ArrayList<String>>) objectMapper
                    .readValue(IOUtils.readFrom(new File(getBackupDbFile(), BACKUP_DATA_STACK)), CopyOnWriteArrayList.class);

            if(tempArrayList.isEmpty()) {
                return;
            }
            
            CopyOnWriteArrayList<String[]> finalArrayList = new CopyOnWriteArrayList<>();
            for(ArrayList<String> arrayData : tempArrayList) {
                finalArrayList.add(arrayData.toArray(new String[0]));
            }

            ConcurrentHashMap<String, LinkedHashMap<String, LongTermData>> tempLongTermDataMap = (ConcurrentHashMap<String, LinkedHashMap<String, LongTermData>>) objectMapper
                    .readValue(IOUtils.readFrom(new File(getBackupDbFile(), BACKUP_DATA_MAP)), ConcurrentHashMap.class);
            ConcurrentHashMap<String, ConcurrentHashMap<String, LongTermData>> finalLongTermDataMap = new ConcurrentHashMap<>();

            for(String uidKey : tempLongTermDataMap.keySet()) {
                LinkedHashMap<String, LongTermData> linkedHashMap = tempLongTermDataMap.get(uidKey);
                ConcurrentHashMap<String, LongTermData> concurrentHashMap = new ConcurrentHashMap<>();
                for(String dataKey : linkedHashMap.keySet()) {
                    concurrentHashMap.put(dataKey, objectMapper.convertValue(linkedHashMap.get(dataKey), LongTermData.class));
                }
                finalLongTermDataMap.put(uidKey, concurrentHashMap);
            }

            CopyOnWriteArrayList<String[]> tempStack = new CopyOnWriteArrayList<>(finalArrayList);
            for(String[] stackObj : tempStack) {
                ConcurrentHashMap<String, LongTermData> userMap = new ConcurrentHashMap<>(finalLongTermDataMap.get(stackObj[0]));
                String dataKey = stackObj[1];
                LongTermData longTermData = userMap.get(dataKey);

                if(!longTermData.isExists()) {
                    userMap.remove(dataKey);
                    finalArrayList.remove(stackObj);
                    finalLongTermDataMap.put(stackObj[0], userMap);
                }
            }

            recentLongTermDataStack = finalArrayList;
            longTermDataMap = finalLongTermDataMap;
        } catch (IOException e) {
            Log.print(LOG_TAG, "Failed to load previous persistent DB from local storage. Running with new blank DB...");
        } finally {
            longTermDataTimeoutWatchThread.start();
        }
    }

    @Override
    public void stopTimeoutWatchThread() {
        if(longTermDataTimeoutWatchThread.isAlive()) {
            backupDataDB();
            longTermDataTimeoutWatchThread.interrupt();
        }
    }

    public synchronized void onLongTermDataReceived(LongTermData longTermData, String uid) throws IOException {
        if(longTermDataMap == null) {
            longTermDataMap = new ConcurrentHashMap<>();
        }

        ConcurrentHashMap<String, LongTermData> userMap;
        if(longTermDataMap.containsKey(uid)) {
            userMap = longTermDataMap.get(uid);
        } else {
            userMap = new ConcurrentHashMap<>();
        }

        userMap.put(longTermData.fileName, longTermData);
        if(longTermArgument.useWebSocketOnTransfer) {
            //TODO: Implement webSocket for large transaction
            //STUB METHOD!!!
            EchoApp.Server.main(new String[]{});
        } else {
            longTermData.write();
        }
        longTermDataMap.put(uid, userMap);

        for(int i = 0; i < recentLongTermDataStack.size(); i++) {
            String[] stackObj = recentLongTermDataStack.get(i);
            if(stackObj[1].equals(longTermData.fileName)) {
                recentLongTermDataStack.remove(i);
                break;
            }
        }
        recentLongTermDataStack.add(getRecentStackObject(longTermData, uid, longTermData.fileName));
    }

    @Nullable
    public synchronized LongTermData onLongTermDataRequested(String uid, String dataKey) throws IOException {
        if(longTermDataMap != null && longTermDataMap.containsKey(uid)) {
            ConcurrentHashMap<String, LongTermData> userMap = longTermDataMap.get(uid);
            if(userMap.containsKey(dataKey)) {
                LongTermData longTermData = userMap.get(dataKey);
                longTermData.read();

                if(longTermArgument.databaseExtendExpireWhenCheckout) {
                    longTermData.updateTimeStamp();
                    userMap.put(dataKey, longTermData);

                    for(int i = 0; i < recentLongTermDataStack.size(); i++) {
                        String[] stackObj = recentLongTermDataStack.get(i);
                        if(stackObj[0].equals(uid) && stackObj[1].equals(dataKey)) {
                            recentLongTermDataStack.set(i, getRecentStackObject(longTermData, uid, longTermData.fileName));
                            break;
                        }
                    }

                    longTermDataMap.put(uid, userMap);
                }

                if(longTermArgument.useWebSocketOnTransfer && longTermArgument.databaseRemoveAfterGet) {
                    userMap.get(dataKey).deletePersist();
                    userMap.remove(dataKey);

                    for(int i = 0; i < recentLongTermDataStack.size(); i++) {
                        String[] stackObj = recentLongTermDataStack.get(i);
                        if(stackObj[0].equals(uid) && stackObj[1].equals(dataKey)) {
                            recentLongTermDataStack.remove(i);
                            break;
                        }
                    }
                    longTermDataMap.put(uid, userMap);
                }

                return longTermData;
            }
        }
        return null;
    }

    private static String[] getRecentStackObject(LongTermData obj, String uid, String dataKey) {
        return new String[]{uid, dataKey, Long.toString(obj.timestamp)};
    }

    private File getBackupDbFile() {
        return new File(longTermArgument.destinationSrcPath, BACKUP_FOLDER);
    }

    private synchronized void backupDataDB() {
        try {
            String longTermDataMapSerialized = new ObjectMapper().writeValueAsString(longTermDataMap);
            String recentLongTermDataStackSerialized = new ObjectMapper().writeValueAsString(recentLongTermDataStack);

            File backupSrc = getBackupDbFile();
            if(!backupSrc.exists() & backupSrc.mkdirs()) {
                Log.printDebug(LOG_TAG, "Created new backup folder: " + backupSrc);
            }

            IOUtils.writeTo(new File(backupSrc, BACKUP_DATA_MAP), longTermDataMapSerialized, true);
            IOUtils.writeTo(new File(backupSrc, BACKUP_DATA_STACK), recentLongTermDataStackSerialized, true);
        } catch (IOException e) {
            Log.print(LOG_TAG, "Failed to back-up persistent DB to local storage");
        }
    }

    private synchronized void performLiveObjGC() {
        Argument serviceArgument = Service.getInstance().getArgument();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

        executor.scheduleAtFixedRate(this::backupDataDB, longTermArgument.databaseBackupInterval, longTermArgument.databaseBackupInterval, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(() -> {
            CopyOnWriteArrayList<String[]> tempStack = new CopyOnWriteArrayList<>(recentLongTermDataStack);
            for(String[] stackObj : tempStack) {
                if(stackObj.length >= 3) {
                    long timestamp = Long.parseLong(stackObj[2]);
                    ConcurrentHashMap<String, LongTermData> userMap = new ConcurrentHashMap<>(longTermDataMap.get(stackObj[0]));
                    String dataKey = stackObj[1];
                    LongTermData longTermData = userMap.get(dataKey);

                    if(longTermArgument.fileBufferGCInterval > 0L && longTermData.isLoaded()
                            && System.currentTimeMillis() - timestamp >= longTermArgument.fileBufferGCInterval) {
                        Log.printDebug(LOG_TAG, String.format("Clean-Up buffered file Data: %s", Arrays.toString(stackObj) + " Freed(Bytes): " + longTermData.tempBuffer.length()));
                        longTermData.flushBuffer();
                        userMap.put(dataKey, longTermData);
                        recentLongTermDataStack.set(recentLongTermDataStack.indexOf(stackObj), getRecentStackObject(longTermData, longTermData.userId, longTermData.fileName));
                        longTermDataMap.put(stackObj[0], userMap);
                        return;
                    }

                    if(System.currentTimeMillis() - timestamp >= longTermArgument.databaseObjLifeTime) {
                        if(serviceArgument.isDebug) {
                            Log.print(LOG_TAG, String.format("Eliminated Data: %s", Arrays.toString(stackObj) + " Remaining: " + (recentLongTermDataStack.size() - 1)));
                        }

                        if(userMap.containsKey(dataKey)) {
                            longTermData.deletePersist();
                            userMap.remove(dataKey);
                            recentLongTermDataStack.remove(stackObj);
                            longTermDataMap.put(stackObj[0], userMap);
                        }
                    }
                }
            }
        }, 0, longTermArgument.databaseGCInterval, TimeUnit.MILLISECONDS);
    }
}
