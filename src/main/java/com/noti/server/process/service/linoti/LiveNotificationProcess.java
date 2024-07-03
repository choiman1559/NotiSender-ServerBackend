package com.noti.server.process.service.linoti;

import com.noti.server.process.Service;
import com.noti.server.process.packet.Device;
import com.noti.server.process.packet.Packet;
import com.noti.server.process.packet.PacketConst;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public class LiveNotificationProcess {

    public static void onLiveNotificationProcess(ApplicationCall call, Map<String, Object> argument) {
        switch ((String) argument.get(PacketConst.KEY_ACTION_TYPE)) {
            case PacketConst.REQUEST_POST_LIVE_NOTIFICATION -> {
                LiveNotificationObj liveNotificationObj = new LiveNotificationObj();
                liveNotificationObj.timestamp = System.currentTimeMillis();
                liveNotificationObj.data = (String) argument.get(PacketConst.KEY_EXTRA_DATA);

                liveNotificationObj.targetDevice = Device.fromMap(argument, true);
                liveNotificationObj.originDevice = Device.fromMap(argument, false);

                if(!liveNotificationObj.originDevice.isEmpty() && !liveNotificationObj.targetDevice.isEmpty()) {
                    LiveNotification.onLiveNotificationDataReceived(liveNotificationObj,
                                                             (String) argument.get(PacketConst.KEY_UID), (String) argument.get(PacketConst.KEY_DATA_KEY));
                    Service.replyPacket(call, Packet.makeNormalPacket());
                } else {
                    Service.replyPacket(call, Packet.makeErrorPacket("Device Information is not available", HttpStatusCode.Companion.getBadRequest()));
                }
            }

            case PacketConst.REQUEST_GET_LIVE_NOTIFICATION -> {
                LiveNotificationObj liveNotificationObj = LiveNotification.onLiveNotificationDataRequested(
                        (String) argument.get(PacketConst.KEY_UID), (String) argument.get(PacketConst.KEY_DATA_KEY));
                if(liveNotificationObj == null) {
                    Service.replyPacket(call, Packet.makeErrorPacket("No matching LiveNotification Data Available", HttpStatusCode.Companion.getBadRequest()));
                } else if(!liveNotificationObj.targetDevice.equals(Device.fromMap(argument, false))
                        || !liveNotificationObj.originDevice.equals(Device.fromMap(argument, true))) {
                    Service.replyPacket(call, Packet.makeErrorPacket("Device Information is invalid comparing from stored data", HttpStatusCode.Companion.getBadRequest()));
                } else {
                    Service.replyPacket(call, Packet.makeNormalPacket(liveNotificationObj.data));
                }
            }

            default -> Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_SERVICE_NOT_FOUND));
        }
    }
}
