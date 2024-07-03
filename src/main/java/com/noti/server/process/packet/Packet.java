package com.noti.server.process.packet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ktor.http.HttpStatusCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Packet {
    @JsonProperty
    String status;
    @JsonProperty
    String errorCause;
    @JsonProperty
    String extraData;
    @JsonIgnore
    HttpStatusCode responseCode;

    public static Packet makeErrorPacket(String errorCause) {
        return makeErrorPacket(errorCause, null, HttpStatusCode.Companion.getInternalServerError());
    }

    public static Packet makeErrorPacket(String errorCause, @Nullable String extraData) {
        return makeErrorPacket(errorCause, extraData, HttpStatusCode.Companion.getInternalServerError());
    }

    public static Packet makeErrorPacket(String errorCause, @NotNull HttpStatusCode responseCode) {
        return makeErrorPacket(errorCause, null, responseCode);
    }

    public static Packet makeErrorPacket(String errorCause, @Nullable String extraData, @NotNull HttpStatusCode responseCode) {
        Packet packet = new Packet();
        packet.status = PacketConst.STATUS_ERROR;
        packet.errorCause = errorCause;
        packet.extraData = extraData == null ? "" : extraData;
        packet.responseCode = responseCode;
        return packet;
    }

    public static Packet makeNormalPacket() {
        return makeNormalPacket("");
    }

    public static Packet makeNormalPacket(@NotNull String extraData) {
        Packet packet = new Packet();
        packet.status = PacketConst.STATUS_OK;
        packet.errorCause = PacketConst.ERROR_NONE;
        packet.extraData = extraData;
        packet.responseCode = HttpStatusCode.Companion.getOK();
        return packet;
    }

    public HttpStatusCode getResponseCode() {
        return responseCode;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
