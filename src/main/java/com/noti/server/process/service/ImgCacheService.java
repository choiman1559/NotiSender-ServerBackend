package com.noti.server.process.service;

import com.noti.server.process.packet.PacketConst;
import com.noti.server.process.service.model.ShortTermProcess;
import com.noti.server.process.service.model.ShortTermTransfer;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public class ImgCacheService extends ShortTermTransfer {
    @Override
    public String getActionTypeName() {
        return PacketConst.SERVICE_TYPE_IMAGE_CACHE;
    }

    @Override
    public void onShortDataProcess(ApplicationCall call, Map<String, Object> argument) {
        super.onShortDataProcess(call, argument);
    }

    @Override
    public ShortTermProcess getShortTermProcess() {
        return super.getShortTermProcess();
    }
}
