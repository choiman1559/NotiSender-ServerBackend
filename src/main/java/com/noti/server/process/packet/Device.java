package com.noti.server.process.packet;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class Device {
    public String deviceName;
    public String deviceId;

    public Device(String deviceName, String deviceId) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Device deviceObj) {
            return Objects.equals(deviceObj.deviceName, this.deviceName)
                    && Objects.equals(deviceObj.deviceId, this.deviceId);
        } else return false;
    }

    public boolean isEmpty() {
        return this.deviceId.isEmpty() || this.deviceName.isEmpty();
    }

    public static Device fromMap(@NotNull Map<String, Object> map, boolean isAlternativeKey) {
        return new Device((String) map.get(isAlternativeKey ? PacketConst.KEY_SEND_DEVICE_NAME : PacketConst.KEY_DEVICE_NAME),
                (String) map.get(isAlternativeKey ? PacketConst.KEY_SEND_DEVICE_ID : PacketConst.KEY_DEVICE_ID));
    }
}
