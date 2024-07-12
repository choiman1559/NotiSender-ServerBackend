package com.noti.server.process;

import com.noti.server.process.packet.Packet;
import com.noti.server.process.service.ImgCacheService;
import com.noti.server.process.service.LiveNotificationService;
import com.noti.server.process.service.model.ShortTermModel;

import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;

import java.util.HashMap;

public class Service {
    private static Service instance;
    private final Argument argument;
    private final HashMap<String, ShortTermModel> shortTermDataList;

    public interface onPacketProcessReplyReceiver {
        void onPacketReply(ApplicationCall call, HttpStatusCode code, String data);
    }

    public onPacketProcessReplyReceiver mOnPacketProcessReplyReceiver;

    private Service(Argument argument) {
        this.argument = argument;
        this.shortTermDataList = new HashMap<>();
    }

    private void addShortTermService(Class<?> cls) throws Exception {
        ShortTermModel shortTermObj = (ShortTermModel) cls.getDeclaredConstructor().newInstance();
        this.shortTermDataList.put(shortTermObj.getActionTypeName(), shortTermObj);
    }

    public static void replyPacket(ApplicationCall call, Packet data) {
        Service mInstance = Service.getInstance();
        if (mInstance != null && mInstance.mOnPacketProcessReplyReceiver != null) {
            mInstance.mOnPacketProcessReplyReceiver.onPacketReply(call, data.getResponseCode(), data.toString());
        }
    }

    public static void configureServiceInstance(Argument argument) {
        instance = new Service(argument);

        try {
            instance.addShortTermService(ImgCacheService.class);
            instance.addShortTermService(LiveNotificationService.class);
        } catch (Exception e) {
            e.printStackTrace();
            //ignore exception
        }
    }

    public static void onServiceAlive() {
        Service service = Service.getInstance();
        for (String key : service.shortTermDataList.keySet()) {
            ShortTermModel shortTermTransfer = service.shortTermDataList.get(key);
            shortTermTransfer.getShortTermProcess().startTimeoutWatchThread();
        }
    }

    public static void onServiceDead() {
        Service service = Service.getInstance();
        for (String key : service.shortTermDataList.keySet()) {
            ShortTermModel shortTermTransfer = service.shortTermDataList.get(key);
            shortTermTransfer.getShortTermProcess().stopTimeoutWatchThread();
        }
    }

    public static Service getInstance() {
        if (instance == null) {
            throw new NullPointerException("Service Instance is not initialized!");
        } else return instance;
    }

    public Argument getArgument() {
        return argument;
    }

    public HashMap<String, ShortTermModel> getShortTermDataList() {
        return shortTermDataList;
    }
}
