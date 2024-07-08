package com.noti.server.process.service.model;

import com.noti.server.process.Service;
import com.noti.server.process.packet.Device;
import com.noti.server.process.packet.Packet;
import com.noti.server.process.packet.PacketConst;

import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public abstract class ShortTermTransfer implements ShortTermModel {
    public final ShortTermProcess shortTermProcess;

    public ShortTermTransfer() {
        this.shortTermProcess = new ShortTermProcess();
    }

    @Override
    public void onShortDataProcess(ApplicationCall call, Map<String, Object> argument) {
        switch ((String) argument.get(PacketConst.KEY_ACTION_TYPE)) {
            case PacketConst.REQUEST_POST_SHORT_TERM_DATA -> {
                ShortTermData shortTermData = new ShortTermData();
                shortTermData.timestamp = System.currentTimeMillis();
                shortTermData.data = (String) argument.get(PacketConst.KEY_EXTRA_DATA);

                shortTermData.targetDevice = Device.fromMap(argument, true);
                shortTermData.originDevice = Device.fromMap(argument, false);

                if(!shortTermData.originDevice.isEmpty() && !shortTermData.targetDevice.isEmpty()) {
                    shortTermProcess.onShortTermDataReceived(shortTermData,
                                                             (String) argument.get(PacketConst.KEY_UID), (String) argument.get(PacketConst.KEY_DATA_KEY));
                    Service.replyPacket(call, Packet.makeNormalPacket());
                } else {
                    Service.replyPacket(call, Packet.makeErrorPacket("Device Information is not available", HttpStatusCode.Companion.getBadRequest()));
                }
            }

            case PacketConst.REQUEST_GET_SHORT_TERM_DATA -> {
                ShortTermData shortTermData = shortTermProcess.onShortTermDataRequested(
                        (String) argument.get(PacketConst.KEY_UID), (String) argument.get(PacketConst.KEY_DATA_KEY));
                if(shortTermData == null) {
                    Service.replyPacket(call, Packet.makeErrorPacket("No matching Data Available", HttpStatusCode.Companion.getBadRequest()));
                } else if(!shortTermData.targetDevice.equals(Device.fromMap(argument, false))
                        || !shortTermData.originDevice.equals(Device.fromMap(argument, true))) {
                    Service.replyPacket(call, Packet.makeErrorPacket("Device Information is invalid comparing from stored data", HttpStatusCode.Companion.getBadRequest()));
                } else {
                    Service.replyPacket(call, Packet.makeNormalPacket(shortTermData.data));
                }
            }

            default -> Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_SERVICE_NOT_FOUND));
        }
    }

    @Override
    public String getActionTypeName() {
        return PacketConst.SERVICE_TYPE_UNKNOWN;
    }

    @Override
    public ShortTermProcess getShortTermProcess() {
        return shortTermProcess;
    }
}
