package com.noti.server.process;

import com.noti.server.process.packet.Packet;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;

public class Service {
    private static Service instance;
    private final Argument argument;

    public interface onPacketProcessReplyReceiver {
        void onPacketReply(ApplicationCall call, HttpStatusCode code, String data);
    }
    public onPacketProcessReplyReceiver mOnPacketProcessReplyReceiver;

    private Service(Argument argument) {
        this.argument = argument;
    }

    public static void replyPacket(ApplicationCall call, Packet data) {
        Service mInstance = Service.getInstance();
        if(mInstance != null && mInstance.mOnPacketProcessReplyReceiver != null) {
            mInstance.mOnPacketProcessReplyReceiver.onPacketReply(call, data.getResponseCode(), data.toString());
        }
    }

    public static void configureServiceInstance(Argument argument) {
        instance = new Service(argument);
    }

    public static Service getInstance() {
        if (instance == null) {
            throw new NullPointerException("Service Instance is not initialized!");
        } else return instance;
    }

    public Argument getArgument() {
        return argument;
    }
}
