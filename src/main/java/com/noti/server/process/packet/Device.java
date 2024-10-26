package com.noti.server.process.packet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class Device {
    @JsonProperty
    public String deviceName;
    @JsonProperty
    public String deviceId;

    @SuppressWarnings("unused")
    public Device() {
        // Default Constructor for Serializer
    }

    public Device(String deviceName, String deviceId) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    @JsonIgnore
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Device deviceObj) {
            return Objects.equals(deviceObj.deviceName, this.deviceName)
                    && Objects.equals(deviceObj.deviceId, this.deviceId);
        } else return false;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return this.deviceId.isEmpty() || this.deviceName.isEmpty();
    }

    @JsonIgnore
    public static Device fromMap(@NotNull Map<String, Object> map, boolean isAlternativeKey) {
        return new Device((String) map.get(isAlternativeKey ? PacketConst.KEY_SEND_DEVICE_NAME : PacketConst.KEY_DEVICE_NAME),
                (String) map.get(isAlternativeKey ? PacketConst.KEY_SEND_DEVICE_ID : PacketConst.KEY_DEVICE_ID));
    }
}
