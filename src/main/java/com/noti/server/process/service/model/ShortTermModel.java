package com.noti.server.process.service.model;

import io.ktor.server.application.ApplicationCall;

import java.util.Map;

public interface ShortTermModel {
    String getActionTypeName();
    void onShortDataProcess(ApplicationCall call, Map<String, Object> argument);
    ShortTermProcess getShortTermProcess();
}
