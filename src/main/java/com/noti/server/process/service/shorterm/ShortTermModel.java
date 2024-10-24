package com.noti.server.process.service.shorterm;

import com.noti.server.process.service.model.ProcessModel;
import com.noti.server.process.service.model.TransferModel;
import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public interface ShortTermModel  extends TransferModel {
    String getActionTypeName();
    void onDataProcess(ApplicationCall call, Map<String, Object> argument);
    ProcessModel getProcess();
    ShortTermArgument getConfigArgument();
}
