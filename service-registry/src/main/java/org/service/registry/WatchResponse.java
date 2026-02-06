package org.service.registry;

import io.etcd.jetcd.Watch;

public record WatchResponse<T>(T watchResponse){
    }


