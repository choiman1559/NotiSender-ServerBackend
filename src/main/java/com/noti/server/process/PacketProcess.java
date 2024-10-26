package com.noti.server.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.noti.server.process.auth.FirebaseHelper;
import com.noti.server.process.packet.Packet;
import com.noti.server.process.packet.PacketConst;
import com.noti.server.process.service.model.TransferModel;

import com.noti.server.process.utils.Log;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class PacketProcess {
    public static void processRequest(ApplicationCall call, String serviceType, @Nullable String idToken, @Nullable String argument) {
        Service service = Service.getInstance();
        Log.printDebug("packetProcess", "RECEIVED " + argument);

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
            Log.printDebug("PacketProcess", String.format("UID: %s, DEVICE: %s",
                    Objects.requireNonNullElse(objectMap.get(PacketConst.KEY_UID), "unknown"),
                    Objects.requireNonNullElse(objectMap.get(PacketConst.KEY_DEVICE_NAME), "empty")));

            processAuth: if(service.getArgument().useAuthentication) {
                if(service.getArgument().allowBlankAuthHeader && (idToken == null || idToken.isEmpty() || idToken.equals("null"))) {
                    break processAuth;
                }

                String uid = (String) objectMap.get(PacketConst.KEY_UID);
                final String bearerPrefix = "Bearer ";

                if(idToken == null || uid == null || uid.isEmpty()) {
                    Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_ILLEGAL_ARGUMENT, HttpStatusCode.Companion.getUnauthorized()));
                    return;
                } else if(!idToken.startsWith(bearerPrefix)) {
                    Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_ILLEGAL_ARGUMENT, HttpStatusCode.Companion.getUnauthorized()));
                    return;
                } else if(!FirebaseHelper.verifyToken(idToken.replace(bearerPrefix, "").trim(), uid)) {
                    Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_ILLEGAL_AUTHENTICATION, HttpStatusCode.Companion.getUnauthorized()));
                    return;
                }
            }

            if(service.getDataTransferServiceList().containsKey(serviceType)) {
                TransferModel dataTransferService = service.getDataTransferServiceList().get(serviceType);
                dataTransferService.onDataProcess(call, objectMap);
            } else switch (serviceType) {
                case PacketConst.SERVICE_TYPE_PING_SERVER -> Service.replyPacket(call, Packet.makeNormalPacket(service.getArgument().matchVersion));
                case PacketConst.SERVICE_TYPE_UNKNOWN -> Packet.makeErrorPacket(PacketConst.ERROR_SERVICE_NOT_IMPLEMENTED);
                default -> Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_SERVICE_NOT_FOUND));
            }
        } else {
            Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_ILLEGAL_ARGUMENT));
        }
    }
}
