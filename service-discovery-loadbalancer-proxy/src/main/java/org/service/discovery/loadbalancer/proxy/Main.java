package org.service.discovery.loadbalancer.proxy;

import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;

import java.net.URI;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Properties configurations = new Properties();

        configurations.setProperty("service.key", System.getenv("SERVICE_KEY"));
        configurations.setProperty("loadbalancer.container.name", System.getenv("LOAD_BALANCER_CONTAINER_NAME"));

        configurations.setProperty("nginx.configuration.template", System.getenv("LOAD_BALANCER_TEMPLATE_FILE"));
        configurations.setProperty("nginx.configuration.actual", System.getenv("LOAD_BALANCER_ACTUAL_FILE"));
        configurations.setProperty("nginx.configuration.backup",System.getenv("LOAD_BALANCER_BACKUP_FILE"));

        configurations.setProperty("etcd.endpoint", System.getenv("ETCD_ENDPOINT"));

        var dockerClient  = DockerClientBuilder.getInstance()
                .withDockerHttpClient(
                        new ApacheDockerHttpClient.Builder()
                                .dockerHost(URI.create("unix:///var/run/docker.sock"))
                                .build()
                ).build();

        Client etcdClient = Client.builder().endpoints((String) configurations.get("etcd.endpoint")).build();
        ByteSequence watchKey = ByteSequence.from(((String) configurations.get("service.key")).getBytes());

        var servicesResponse = etcdClient.getKVClient().get(watchKey, GetOption.builder().isPrefix(true).build()).get();


        Queue<UpdateCommand> updateCommandQueue = new ConcurrentLinkedQueue<>();

        Consumer<WatchResponse> serviceEventListener = new ServiceEventListener((String) configurations.get("service.key"), updateCommandQueue);

        LoadBalancerUpdater nginxUpdater = new DockerContainerNginxUpdater(new ServiceList(servicesResponse.getKvs(), servicesResponse.getCount()),configurations, (String) configurations.get("loadbalancer.container.name"), dockerClient);


        var loadBalancerUpdaterExecutor = new LoadBalancerUpdaterQueueExecutor(updateCommandQueue, nginxUpdater, (queue, loadBalancerUpdater) -> {
                UpdateCommand cmd = queue.poll();
                if(cmd != null) {
                    loadBalancerUpdater.onUpdateCommand(cmd);
                }
                Thread.sleep(500);
        });

        var watcher = etcdClient.getWatchClient().watch(watchKey, WatchOption.builder().isPrefix(true).withPrevKV(true).build(), serviceEventListener);

        loadBalancerUpdaterExecutor.assignEventLoopToPlatformThread();
        loadBalancerUpdaterExecutor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(!watcher.isClosed()) watcher.close();
            etcdClient.close();
            loadBalancerUpdaterExecutor.stop();
        }));
    }
}