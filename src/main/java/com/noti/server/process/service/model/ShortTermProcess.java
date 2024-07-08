package com.noti.server.process.service.model;

import com.noti.server.process.Argument;
import com.noti.server.process.Service;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.*;

public class ShortTermProcess {

    public volatile ConcurrentHashMap<String, ConcurrentHashMap<String, ShortTermData>> shortTermDataMap;
    public volatile CopyOnWriteArrayList<String[]> recentShortTermDataStack = new CopyOnWriteArrayList<>();
    private final Thread shortTermDataTimeoutWatchThread = new Thread(this::performLiveObjGC);

    public void startTimeoutWatchThread() {
        shortTermDataTimeoutWatchThread.start();
    }

    public void stopTimeoutWatchThread() {
        if(shortTermDataTimeoutWatchThread.isAlive()) {
            shortTermDataTimeoutWatchThread.interrupt();
        }
    }

    public synchronized void onShortTermDataReceived(ShortTermData shortTermData, String uid, String dataKey) {
        if(shortTermDataMap == null) {
            shortTermDataMap = new ConcurrentHashMap<>();
        }

        ConcurrentHashMap<String, ShortTermData> userMap;
        if(shortTermDataMap.containsKey(uid)) {
            userMap = shortTermDataMap.get(uid);
        } else {
            userMap = new ConcurrentHashMap<>();
        }

        userMap.put(dataKey, shortTermData);
        shortTermDataMap.put(uid, userMap);

        for(int i = 0; i < recentShortTermDataStack.size(); i++) {
            String[] stackObj = recentShortTermDataStack.get(i);
            if(stackObj[1].equals(dataKey)) {
                recentShortTermDataStack.remove(i);
                break;
            }
        }
        recentShortTermDataStack.add(getRecentStackObject(shortTermData, uid, dataKey));
    }

    @Nullable
    public synchronized ShortTermData onShortTermDataRequested(String uid, String dataKey) {
        if(shortTermDataMap != null && shortTermDataMap.containsKey(uid)) {
            ConcurrentHashMap<String, ShortTermData> userMap = shortTermDataMap.get(uid);
            if(userMap.containsKey(dataKey)) {
                ShortTermData shortTermData = userMap.get(dataKey);
                userMap.remove(dataKey);

                for(int i = 0; i < recentShortTermDataStack.size(); i++) {
                    String[] stackObj = recentShortTermDataStack.get(i);
                    if(stackObj[0].equals(uid) && stackObj[1].equals(dataKey)) {
                        recentShortTermDataStack.remove(i);
                        break;
                    }
                }

                shortTermDataMap.put(uid, userMap);
                return shortTermData;
            }
        }
        return null;
    }

    private static String[] getRecentStackObject(ShortTermData obj, String uid, String dataKey) {
        return new String[]{uid, dataKey, Long.toString(obj.timestamp)};
    }

    private synchronized void performLiveObjGC() {
        Argument serviceArgument = Service.getInstance().getArgument();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            CopyOnWriteArrayList<String[]> tempStack = new CopyOnWriteArrayList<>(recentShortTermDataStack);
            for(String[] stackObj : tempStack) {
                if(stackObj.length >= 3) {
                    long timestamp = Long.parseLong(stackObj[2]);
                    if(System.currentTimeMillis() - timestamp >= serviceArgument.liveNotificationDatabaseObjLifeTime) {
                        if(serviceArgument.isDebug) {
                            System.out.printf("Eliminated unused Data: %s\n", Arrays.toString(stackObj));
                        }

                        ConcurrentHashMap<String, ShortTermData> userMap = new ConcurrentHashMap<>(shortTermDataMap.get(stackObj[0]));
                        if(userMap.containsKey(stackObj[1])) {
                            userMap.remove(stackObj[1]);
                            recentShortTermDataStack.remove(stackObj);
                            shortTermDataMap.put(stackObj[0], userMap);
                        }
                    }
                }
            }
        }, 0, serviceArgument.liveNotificationDatabaseGCInterval, TimeUnit.MILLISECONDS);
    }
}