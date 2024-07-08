package com.noti.server.process.service.model;

import com.noti.server.process.packet.Device;

public class ShortTermData {
    Device originDevice;
    Device targetDevice;

    String data;
    long timestamp;
}
