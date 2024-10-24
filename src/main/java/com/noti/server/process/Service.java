package com.noti.server.process;

import com.noti.server.process.auth.FirebaseHelper;
import com.noti.server.process.packet.Packet;
import com.noti.server.process.service.FileListService;
import com.noti.server.process.service.ImgCacheService;
import com.noti.server.process.service.LiveNotificationService;
import com.noti.server.process.service.PacketProxyService;
import com.noti.server.process.service.model.TransferModel;

import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;

import java.util.HashMap;

public class Service {
    private static Service instance;
    private final Argument argument;
    private final HashMap<String, TransferModel> dataTransferServiceList;

    public interface onPacketProcessReplyReceiver {
        void onPacketReply(ApplicationCall call, HttpStatusCode code, String data);
    }

    public onPacketProcessReplyReceiver mOnPacketProcessReplyReceiver;

    private Service(Argument argument) {
        this.argument = argument;
        this.dataTransferServiceList = new HashMap<>();
    }

    private void addDataTransferService(Class<?> cls) throws Exception {
        TransferModel dataTransferService = (TransferModel) cls.getDeclaredConstructor().newInstance();
        this.dataTransferServiceList.put(dataTransferService.getActionTypeName(), dataTransferService);
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
            FirebaseHelper.init(argument.authCredentialPath);
            instance.addDataTransferService(ImgCacheService.class);
            instance.addDataTransferService(LiveNotificationService.class);
            instance.addDataTransferService(PacketProxyService.class);
            instance.addDataTransferService(FileListService.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onServiceAlive() {
        Service service = Service.getInstance();
        for (String key : service.dataTransferServiceList.keySet()) {
            TransferModel transferService = service.dataTransferServiceList.get(key);
            transferService.getProcess().startTimeoutWatchThread();
        }
    }

    public static void onServiceDead() {
        Service service = Service.getInstance();
        for (String key : service.dataTransferServiceList.keySet()) {
            TransferModel transferService = service.dataTransferServiceList.get(key);
            transferService.getProcess().stopTimeoutWatchThread();
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

    public HashMap<String, TransferModel> getDataTransferServiceList() {
        return dataTransferServiceList;
    }
}
