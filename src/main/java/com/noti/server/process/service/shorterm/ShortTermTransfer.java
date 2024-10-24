package com.noti.server.process.service.shorterm;

import com.noti.server.process.Service;
import com.noti.server.process.packet.Device;
import com.noti.server.process.packet.Packet;
import com.noti.server.process.packet.PacketConst;

import com.noti.server.process.service.model.ProcessModel;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public abstract class ShortTermTransfer implements ShortTermModel {
    public final ShortTermProcess shortTermProcess;
    public final ShortTermArgument shortTermArgument;

    public ShortTermTransfer() {
        if(getConfigArgument() == null) {
            throw new IllegalArgumentException("ShortTermArgument must not be null!");
        }

        this.shortTermArgument = getConfigArgument();
        this.shortTermProcess = new ShortTermProcess(getActionTypeName(), this.shortTermArgument);
    }

    @Override
    public void onDataProcess(ApplicationCall call, Map<String, Object> argument) {
        switch ((String) argument.get(PacketConst.KEY_ACTION_TYPE)) {
            case PacketConst.REQUEST_POST_SHORT_TERM_DATA -> {
                ShortTermData shortTermData = new ShortTermData();
                shortTermData.timestamp = System.currentTimeMillis();
                shortTermData.data = (String) argument.get(PacketConst.KEY_EXTRA_DATA);

                shortTermData.originDevice = Device.fromMap(argument, false);
                if(shortTermArgument.databaseCheckReceiveDeviceId) {
                    shortTermData.targetDevice = Device.fromMap(argument, true);
                }

                if(!shortTermData.originDevice.isEmpty() && !(shortTermArgument.databaseCheckReceiveDeviceId && shortTermData.targetDevice.isEmpty())) {
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
                } else if((shortTermArgument.databaseCheckReceiveDeviceId
                        && !shortTermData.targetDevice.equals(Device.fromMap(argument, false)))
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
    public ProcessModel getProcess() {
        return shortTermProcess;
    }

    @Override
    public ShortTermArgument getConfigArgument() {
        return null;
    }
}
