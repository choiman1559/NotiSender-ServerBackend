package com.noti.server.process.service;

import com.noti.server.process.Service;
import com.noti.server.process.packet.PacketConst;
import com.noti.server.process.service.model.ProcessModel;
import com.noti.server.process.service.shorterm.ShortTermArgument;
import com.noti.server.process.service.shorterm.ShortTermTransfer;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public class LiveNotificationService extends ShortTermTransfer {
    @Override
    public String getActionTypeName() {
        return PacketConst.SERVICE_TYPE_LIVE_NOTIFICATION;
    }

    @Override
    public void onDataProcess(ApplicationCall call, Map<String, Object> argument) {
        super.onDataProcess(call, argument);
    }

    @Override
    public ProcessModel getProcess() {
        return super.getProcess();
    }

    @Override
    public ShortTermArgument getConfigArgument() {
        return Service.getInstance().getArgument().liveNotiArgument;
    }
}
