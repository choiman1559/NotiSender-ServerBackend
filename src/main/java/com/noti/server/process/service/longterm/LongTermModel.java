package com.noti.server.process.service.longterm;

import com.noti.server.process.service.model.ProcessModel;
import com.noti.server.process.service.model.TransferModel;

public interface LongTermModel extends TransferModel {
    String getActionTypeName();
    ProcessModel getProcess();
    LongTermArgument getConfigArgument();
}
