package com.noti.server.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.noti.server.process.packet.Packet;
import com.noti.server.process.packet.PacketConst;
import com.noti.server.process.service.model.ShortTermModel;

import io.ktor.server.application.ApplicationCall;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PacketProcess {
    public static void processRequest(ApplicationCall call, String serviceType, @Nullable String argument) {
        Service service = Service.getInstance();
        if(service.getArgument().isDebug)
            System.out.println("args:" + argument);

        Map<String, Object> objectMap = null;
        try {
            if(argument != null && !argument.isEmpty()) {
                //noinspection unchecked
                objectMap = new ObjectMapper().readValue(argument, Map.class);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if(objectMap != null) {
            if(service.getShortTermDataList().containsKey(serviceType)) {
                ShortTermModel shortTermTransfer = service.getShortTermDataList().get(serviceType);
                shortTermTransfer.onShortDataProcess(call, objectMap);
            } else switch (serviceType) {
                case PacketConst.SERVICE_TYPE_PING_SERVER -> Service.replyPacket(call, Packet.makeNormalPacket(service.getArgument().matchVersion));
                case PacketConst.SERVICE_TYPE_FILE_TRANSFER -> //TODO: Noting to do for now;
                        Packet.makeErrorPacket(PacketConst.ERROR_SERVICE_NOT_IMPLEMENTED);
                default -> Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_SERVICE_NOT_FOUND));
            }
        } else {
            Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_ILLEGAL_ARGUMENT));
        }
    }
}
