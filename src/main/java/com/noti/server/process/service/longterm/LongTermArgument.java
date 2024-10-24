package com.noti.server.process.service.longterm;

public class LongTermArgument {
    public boolean databaseRemoveAfterGet;
    public boolean databaseCheckReceiveDeviceId;
    public boolean useWebSocketOnTransfer;
    public boolean databaseExtendExpireWhenCheckout;
    public long fileBufferGCInterval;
    public long databaseGCInterval;
    public long databaseObjLifeTime;
    public String destinationSrcPath;
}
