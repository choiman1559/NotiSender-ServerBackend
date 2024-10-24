package com.noti.server.process.service.longterm;

import com.noti.server.module.EchoApp;
import com.noti.server.process.Service;
import com.noti.server.process.packet.Device;
import com.noti.server.process.packet.Packet;
import com.noti.server.process.packet.PacketConst;
import com.noti.server.process.service.model.ProcessModel;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;

import java.io.IOException;
import java.util.Map;

public abstract class LongTermTransfer implements LongTermModel {
    public final LongTermProcess longTermProcess;
    public final LongTermArgument longTermArgument;

    public LongTermTransfer() {
        if(getConfigArgument() == null) {
            throw new IllegalArgumentException("ShortTermArgument must not be null!");
        }

        this.longTermArgument = getConfigArgument();
        this.longTermProcess = new LongTermProcess(getActionTypeName(), this.longTermArgument);
    }

    @Override
    public void onDataProcess(ApplicationCall call, Map<String, Object> argument) {
        final String userId = (String) argument.get(PacketConst.KEY_UID);
        final String fileName =  (String) argument.get(PacketConst.KEY_DATA_KEY);

        switch ((String) argument.get(PacketConst.KEY_ACTION_TYPE)) {
            case PacketConst.REQUEST_POST_SHORT_TERM_DATA -> {
                LongTermData longTermData = new LongTermData();
                longTermData.timestamp = System.currentTimeMillis();
                longTermData.userName = userId;

                longTermData.fileName = fileName;
                longTermData.parentDest = longTermArgument.destinationSrcPath;
                longTermData.tempBuffer = (String) argument.get(PacketConst.KEY_EXTRA_DATA);

                longTermData.originDevice = Device.fromMap(argument, false);
                if(longTermArgument.databaseCheckReceiveDeviceId) {
                    longTermData.targetDevice = Device.fromMap(argument, true);
                }

                if(!longTermData.originDevice.isEmpty() && !(longTermArgument.databaseCheckReceiveDeviceId && longTermData.targetDevice.isEmpty())) {
                    try {
                        longTermProcess.onLongTermDataReceived(longTermData, userId);
                        Service.replyPacket(call, Packet.makeNormalPacket());
                    } catch (IOException e) {
                        Service.replyPacket(call, Packet.makeErrorPacket("IO Failed while writing to persistent db", HttpStatusCode.Companion.getInternalServerError()));
                    }
                } else {
                    Service.replyPacket(call, Packet.makeErrorPacket("Device Information is not available", HttpStatusCode.Companion.getBadRequest()));
                }
            }

            case PacketConst.REQUEST_GET_SHORT_TERM_DATA -> {
                try {
                    LongTermData longTermData = longTermProcess.onLongTermDataRequested(userId, fileName);
                    if(longTermData == null) {
                        Service.replyPacket(call, Packet.makeErrorPacket("No matching Data Available", HttpStatusCode.Companion.getBadRequest()));
                    } else if((longTermArgument.databaseCheckReceiveDeviceId
                            && !longTermData.targetDevice.equals(Device.fromMap(argument, false)))
                            || !longTermData.originDevice.equals(Device.fromMap(argument, true))) {
                        Service.replyPacket(call, Packet.makeErrorPacket("Device Information is invalid comparing from stored data", HttpStatusCode.Companion.getBadRequest()));
                    } else if(longTermArgument.useWebSocketOnTransfer) {
                        //TODO: Implement socket IO
                        EchoApp.Server.main(new String[0]);
                    } else {
                        Service.replyPacket(call, Packet.makeNormalPacket(longTermData.tempBuffer));
                    }
                } catch (Exception e) {
                    Service.replyPacket(call, Packet.makeErrorPacket("IO Failed while reading from stored db", HttpStatusCode.Companion.getInternalServerError()));
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
        return longTermProcess;
    }

    @Override
    public LongTermArgument getConfigArgument() {
        return null;
    }
}
