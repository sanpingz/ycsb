package com.yahoo.ycsb.config.memcached;

import net.spy.memcached.FailureMode;
import net.spy.memcached.ReplicateTo;

public interface MemcachedCompatibleConfig {

    String getHosts();

    boolean getCheckOperationStatus();

    long getOpTimeout();

    int getReadBufferSize();

    FailureMode getFailureMode();

    long getShutdownTimeoutMillis();

    int getObjectExpirationTime();

}
