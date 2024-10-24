package com.noti.server.process.service;

import com.noti.server.process.Service;
import com.noti.server.process.packet.PacketConst;
import com.noti.server.process.service.longterm.LongTermArgument;
import com.noti.server.process.service.longterm.LongTermTransfer;
import com.noti.server.process.service.model.ProcessModel;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public class FileListService extends LongTermTransfer {
    @Override
    public String getActionTypeName() {
        return PacketConst.SERVICE_TYPE_FILE_LIST;
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
    public LongTermArgument getConfigArgument() {
        return Service.getInstance().getArgument().fileListArgument;
    }
}
