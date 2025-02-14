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
            case PacketConst.REQUEST_POST_LONG_TERM_DATA -> {
                LongTermData longTermData = new LongTermData(userId);
                longTermData.timestamp = System.currentTimeMillis();

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
                        Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_DATA_DB_IO_FAILED_WRITE, HttpStatusCode.Companion.getInternalServerError()));
                        if(Service.getInstance().getArgument().isDebug) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_DATA_DEVICE_INFO_NOT_AVAILABLE, HttpStatusCode.Companion.getBadRequest()));
                }
            }

            case PacketConst.REQUEST_GET_LONG_TERM_DATA -> {
                try {
                    LongTermData longTermData = longTermProcess.onLongTermDataRequested(userId, fileName);
                    if(longTermData == null) {
                        Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_DATA_NO_MATCHING_DATA, HttpStatusCode.Companion.getBadRequest()));
                    } else if((longTermArgument.databaseCheckReceiveDeviceId
                            && !longTermData.targetDevice.equals(Device.fromMap(argument, false)))
                            || !longTermData.originDevice.equals(Device.fromMap(argument, true))) {
                        Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_DATA_DEVICE_INFO_NOT_MATCH, HttpStatusCode.Companion.getBadRequest()));
                    } else if(longTermArgument.useWebSocketOnTransfer) {
                        //TODO: Implement socket IO
                        EchoApp.Server.main(new String[0]);
                    } else {
                        Service.replyPacket(call, Packet.makeNormalPacket(longTermData.tempBuffer));
                    }
                } catch (Exception e) {
                    Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_DATA_DB_IO_FAILED_READ, HttpStatusCode.Companion.getInternalServerError()));
                    if(Service.getInstance().getArgument().isDebug) {
                        e.printStackTrace();
                    }
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
