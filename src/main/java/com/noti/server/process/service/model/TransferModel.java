package com.noti.server.process.service.model;

import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public interface TransferModel {
    String getActionTypeName();
    void onDataProcess(ApplicationCall call, Map<String, Object> argument);
    ProcessModel getProcess();
    Object getConfigArgument();
}
