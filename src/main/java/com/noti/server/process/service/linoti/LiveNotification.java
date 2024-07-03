package com.noti.server.process.service.linoti;

import com.noti.server.process.Argument;
import com.noti.server.process.Service;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;

public class LiveNotification {

    public volatile static ConcurrentHashMap<String, ConcurrentHashMap<String, LiveNotificationObj>> liveNotificationMap;
    public volatile static CopyOnWriteArrayList<String[]> recentLiveNotificationStack = new CopyOnWriteArrayList<>();
    private static final Thread liveNotificationTimeoutWatchThread = new Thread(LiveNotification::performLiveObjGC);

    public static void startTimeoutWatchThread() {
        liveNotificationTimeoutWatchThread.start();
    }

    public static void stopTimeoutWatchThread() {
        if(liveNotificationTimeoutWatchThread.isAlive()) {
            liveNotificationTimeoutWatchThread.interrupt();
        }
    }

    public synchronized static void onLiveNotificationDataReceived(LiveNotificationObj liveNotificationObj, String uid, String dataKey) {
        if(liveNotificationMap == null) {
            liveNotificationMap = new ConcurrentHashMap<>();
        }

        ConcurrentHashMap<String, LiveNotificationObj> userMap;
        if(liveNotificationMap.containsKey(uid)) {
            userMap = liveNotificationMap.get(uid);
        } else {
            userMap = new ConcurrentHashMap<>();
        }

        userMap.put(dataKey, liveNotificationObj);
        liveNotificationMap.put(uid, userMap);

        for(int i = 0;i < recentLiveNotificationStack.size(); i++) {
            String[] stackObj = recentLiveNotificationStack.get(i);
            if(stackObj[1].equals(dataKey)) {
                recentLiveNotificationStack.remove(i);
                break;
            }
        }
        recentLiveNotificationStack.add(getRecentStackObject(liveNotificationObj, uid, dataKey));
    }

    @Nullable
    public synchronized static LiveNotificationObj onLiveNotificationDataRequested(String uid, String dataKey) {
        if(liveNotificationMap != null && liveNotificationMap.containsKey(uid)) {
            ConcurrentHashMap<String, LiveNotificationObj> userMap = liveNotificationMap.get(uid);
            if(userMap.containsKey(dataKey)) {
                LiveNotificationObj liveNotificationObj = userMap.get(dataKey);
                userMap.remove(dataKey);

                for(int i = 0;i < recentLiveNotificationStack.size(); i++) {
                    String[] stackObj = recentLiveNotificationStack.get(i);
                    if(stackObj[0].equals(uid) && stackObj[1].equals(dataKey)) {
                        recentLiveNotificationStack.remove(i);
                        break;
                    }
                }

                liveNotificationMap.put(uid, userMap);
                return liveNotificationObj;
            }
        }
        return null;
    }

    private static String[] getRecentStackObject(LiveNotificationObj obj, String uid, String dataKey) {
        return new String[]{uid, dataKey, Long.toString(obj.timestamp)};
    }

    private synchronized static void performLiveObjGC() {
        Argument serviceArgument = Service.getInstance().getArgument();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            CopyOnWriteArrayList<String[]> tempStack = new CopyOnWriteArrayList<>(recentLiveNotificationStack);
            for(String[] stackObj : tempStack) {
                if(stackObj.length >= 3) {
                    long timestamp = Long.parseLong(stackObj[2]);
                    if(System.currentTimeMillis() - timestamp >= serviceArgument.liveNotificationDatabaseObjLifeTime) {
                        if(serviceArgument.isDebug) {
                            System.out.printf("Eliminated unused live notification data: %s\n", Arrays.toString(stackObj));
                        }

                        ConcurrentHashMap<String, LiveNotificationObj> userMap = new ConcurrentHashMap<>(liveNotificationMap.get(stackObj[0]));
                        if(userMap.containsKey(stackObj[1])) {
                            userMap.remove(stackObj[1]);
                            recentLiveNotificationStack.remove(stackObj);
                            liveNotificationMap.put(stackObj[0], userMap);
                        }
                    }
                }
            }
        }, 0, serviceArgument.liveNotificationDatabaseGCInterval, TimeUnit.MILLISECONDS);
    }
}
