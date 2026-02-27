package org.service.discovery.loadbalancer.proxy;

import io.etcd.jetcd.Watch;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;

import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.function.Consumer;

public class ServiceEventListener implements Consumer<WatchResponse> {

    private final String key;
    private final Queue<UpdateCommand> updaterQueue;

    public ServiceEventListener(String key, Queue<UpdateCommand> updaterQueue) {
        this.key = key;
        this.updaterQueue = updaterQueue;
    }

    @Override
    public void accept(WatchResponse watchResponse) {
        var events = watchResponse.getEvents();
        for(WatchEvent event : events) {
            if(event.getEventType().equals(WatchEvent.EventType.DELETE)){
                var prevKV = event.getPrevKV();
                if(prevKV != null && prevKV.getKey().toString(StandardCharsets.UTF_8).startsWith(key)){
                    updaterQueue.offer(new UpdateCommand.DeleteCommand(prevKV.getValue()));
                }
            }
            else if(event.getEventType().equals(WatchEvent.EventType.PUT)) {
                updaterQueue.offer(new UpdateCommand.PutCommand(event.getKeyValue().getValue()));
            }
        }

    }
}
