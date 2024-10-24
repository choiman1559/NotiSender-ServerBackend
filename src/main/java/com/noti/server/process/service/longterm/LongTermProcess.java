package com.noti.server.process.service.longterm;

import com.noti.server.module.EchoApp;
import com.noti.server.process.Argument;
import com.noti.server.process.Log;
import com.noti.server.process.Service;
import com.noti.server.process.service.model.ProcessModel;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.*;

public class LongTermProcess implements ProcessModel {

    private final String LOG_TAG;
    public final LongTermArgument longTermArgument;
    public volatile ConcurrentHashMap<String, ConcurrentHashMap<String, LongTermData>> longTermDataMap;
    public volatile CopyOnWriteArrayList<String[]> recentLongTermDataStack = new CopyOnWriteArrayList<>();
    private final Thread longTermDataTimeoutWatchThread = new Thread(this::performLiveObjGC);

    public LongTermProcess(String LOG_TAG, LongTermArgument longTermArgument) {
        this.LOG_TAG = LOG_TAG;
        this.longTermArgument = longTermArgument;
    }

    @Override
    public void startTimeoutWatchThread() {
        longTermDataTimeoutWatchThread.start();
    }

    @Override
    public void stopTimeoutWatchThread() {
        if(longTermDataTimeoutWatchThread.isAlive()) {
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
                    userMap.put(uid, longTermData);

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

    private synchronized void performLiveObjGC() {
        Argument serviceArgument = Service.getInstance().getArgument();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            CopyOnWriteArrayList<String[]> tempStack = new CopyOnWriteArrayList<>(recentLongTermDataStack);
            for(String[] stackObj : tempStack) {
                if(stackObj.length >= 3) {
                    long timestamp = Long.parseLong(stackObj[2]);
                    ConcurrentHashMap<String, LongTermData> userMap = new ConcurrentHashMap<>(longTermDataMap.get(stackObj[0]));
                    String dataKey = stackObj[1];
                    LongTermData longTermData = userMap.get(dataKey);

                    if(longTermArgument.fileBufferGCInterval > 0L
                            && System.currentTimeMillis() - timestamp >= longTermArgument.fileBufferGCInterval) {
                        if (serviceArgument.isDebug) {
                            Log.print(LOG_TAG, String.format("Clean-Up buffered file Data: %s", Arrays.toString(stackObj) + " Freed(Bytes): " + longTermData.tempBuffer.length()));
                        }
                        longTermData.flushBuffer();
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
