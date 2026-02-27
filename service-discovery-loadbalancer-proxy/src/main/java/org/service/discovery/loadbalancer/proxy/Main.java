package org.service.discovery.loadbalancer.proxy;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;

import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
//        Properties configurations = new Properties();
//
//        Queue<UpdateCommand> updateCommandQueue = new ConcurrentLinkedQueue<>();
//        Consumer<WatchResponse> serviceEventListener = new ServiceEventListener((String) configurations.get("service.key"), updateCommandQueue);
//        LoadBalancerUpdater loadBalancerUpdater = new DockerContainerNginxUpdater(configurations);
//        Client etcdClient = Client.builder().build();
//        ByteSequence watchKey = ByteSequence.from(((String) configurations.get("service.key")).getBytes());
//
//        etcdClient.getWatchClient().watch(watchKey, WatchOption.builder().isPrefix(true).build(), serviceEventListener);
//
//        Thread.ofPlatform().start(() -> {
//            while(true) {
//                UpdateCommand cmd = updateCommandQueue.poll();
//                if(cmd != null) {
//                    loadBalancerUpdater.onUpdateCommand(cmd);
//                } else {
//                    Thread.yield();
//                }
//            }
//        });
    }
}