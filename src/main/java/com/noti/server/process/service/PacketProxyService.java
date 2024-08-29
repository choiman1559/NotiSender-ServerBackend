package com.noti.server.process.service;

import com.noti.server.process.Service;
import com.noti.server.process.packet.PacketConst;
import com.noti.server.process.service.model.ShortTermArgument;
import com.noti.server.process.service.model.ShortTermProcess;
import com.noti.server.process.service.model.ShortTermTransfer;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public class PacketProxyService extends ShortTermTransfer {
    @Override
    public String getActionTypeName() {
        return PacketConst.SERVICE_TYPE_PACKET_PROXY;
    }

    @Override
    public void onShortDataProcess(ApplicationCall call, Map<String, Object> argument) {
        super.onShortDataProcess(call, argument);
    }

    @Override
    public ShortTermProcess getShortTermProcess() {
        return super.getShortTermProcess();
    }

    @Override
    public ShortTermArgument getConfigArgument() {
        return Service.getInstance().getArgument().imageCacheArgument;
    }
}
