package org.service.discovery.loadbalancer.proxy;

import com.dslplatform.json.DslJson;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.model.Frame;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DockerContainerNginxUpdater implements LoadBalancerUpdater{
    private final Map<String, Service> services = new HashMap<>();
    private final DslJson<ServiceJsonDataMapper> dslJson = new DslJson<>();
    private final StringBuilder servicesStringBuilder = new StringBuilder();
    private final Properties configuration;
    private final DockerClient dockerClient;
    private final String containerName;
    public DockerContainerNginxUpdater(Properties configuration, String containerName, DockerClient dockerClient) {
        this.configuration = configuration;
        this.containerName = containerName;
        this.dockerClient = dockerClient;
    }

    @Override
    public void onUpdateCommand(UpdateCommand updateCommand) {
        switch (updateCommand){
            case UpdateCommand.PutCommand putCommand  -> {
                try {
                    put(putCommand);
                } catch (IOException e) {
                    rollback(e);
                } finally {
                    servicesStringBuilder.setLength(0);
                }
            }
            case UpdateCommand.DeleteCommand deleteCommand -> {
                try {
                    delete(deleteCommand);
                } catch (IOException e) {
                    rollback(e);
                } finally {
                    servicesStringBuilder.setLength(0);
                }
            }
            default -> throw new IllegalArgumentException("Unknown command");
        }
    }

    private void put(UpdateCommand.PutCommand putCommand) throws IOException {
        Service service = deserializeToService(putCommand.value().toString().getBytes(StandardCharsets.UTF_8));
        if(services.containsKey(service.serviceId())){
            throw new IllegalStateException("Service key exist in Map");
        }
        services.put(service.serviceId(), service);

        updateConfiguration(services);
        var isValid = validateAndReload();
        if(!isValid) {
            throw new IllegalStateException("Validation Failed !");
        }
//        servicesStringBuilder.setLength(0);

    }
    private void delete(UpdateCommand.DeleteCommand deleteCommand) throws IOException {

        Service service = deserializeToService(deleteCommand.value().toString().getBytes(StandardCharsets.UTF_8));
        if(!services.containsKey(service.serviceId())){
            throw new IllegalStateException("Service key does not exist in Map");
        }
        services.remove(service.serviceId());

        updateConfiguration(services);

        var isValid = validateAndReload();

        if(!isValid) {
            throw new IllegalStateException("Validation Failed !");
        }
//        servicesStringBuilder.setLength(0);
    }

    private void rollback(IOException e) {
        try {
            Path actualPath = Path.of((String) configuration.get("nginx.configuration.actual"));
            Path backupPath = Path.of((String) configuration.get("nginx.configuration.backup"));

            if (Files.exists(backupPath)) {
                Files.move(
                        backupPath,
                        actualPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            }
        } catch (Exception rollbackEx) {
            System.err.println(rollbackEx.getMessage());

        }

        System.err.println("Config update failed. Rollback attempted.");
        System.err.println(Arrays.toString(e.getStackTrace()));

    }

    private Service deserializeToService(byte[] json) throws IOException {
        var serviceJsonPOJO =  dslJson.deserialize(ServiceJsonDataMapper.class, json, json.length);
        assert serviceJsonPOJO != null;
        return new Service(serviceJsonPOJO.serviceId, serviceJsonPOJO.serviceName, serviceJsonPOJO.ip, serviceJsonPOJO.port, serviceJsonPOJO.protocol);
    }

    private void updateConfigurationWhenServiceListEmpty() throws IOException {
        servicesStringBuilder.append("server 127.0.0.1:65535;");
        Path templatePath = Path.of((String) configuration.get("nginx.configuration.template"));
        Path actualPath = Path.of((String) configuration.get("nginx.configuration.actual"));
        Path backupPath = Path.of((String) configuration.get("nginx.configuration.backup"));
        Path tempPath = actualPath.resolveSibling(actualPath.getFileName() + ".tmp");

        var nginxTemplate = Files.readString(templatePath);
        nginxTemplate = nginxTemplate.replace("{{Services}}", servicesStringBuilder.toString());

        try (var channel = FileChannel.open(
                tempPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            channel.write(StandardCharsets.UTF_8.encode(nginxTemplate));
            channel.force(true);
        }

        if (Files.exists(actualPath)) {
            Files.move(
                    actualPath,
                    backupPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        }

        Files.move(
                tempPath,
                actualPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );

    }
    private void updateConfigurationWhenServiceListNotEmpty(List<Service> servicesImmutableSnapshot) throws IOException {
        for (Service s : servicesImmutableSnapshot) {
            servicesStringBuilder
                    .append("server")
                    .append(" ")
                    .append(s.ip())
                    .append(":")
                    .append(s.port())
                    .append(";")
                    .append("\n\t\t");

        }

        Path templatePath = Path.of((String) configuration.get("nginx.configuration.template"));
        Path actualPath = Path.of((String) configuration.get("nginx.configuration.actual"));
        Path backupPath = Path.of((String) configuration.get("nginx.configuration.backup"));
        Path tempPath = actualPath.resolveSibling(actualPath.getFileName() + ".tmp");

        var nginxTemplate = Files.readString(templatePath);
        nginxTemplate = nginxTemplate.replace("{{Services}}", servicesStringBuilder.toString());

        try (var channel = FileChannel.open(
                tempPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            channel.write(StandardCharsets.UTF_8.encode(nginxTemplate));
            channel.force(true);
        }

        if (Files.exists(actualPath)) {
            Files.move(
                    actualPath,
                    backupPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        }

        Files.move(
                tempPath,
                actualPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );

    }
    private void updateConfiguration(Map<String, Service> services) throws IOException {
        var servicesImmutableSnapshot = List.copyOf(services.values());

        if(servicesImmutableSnapshot.isEmpty()) {
            updateConfigurationWhenServiceListEmpty();
        } else {
            updateConfigurationWhenServiceListNotEmpty(servicesImmutableSnapshot);
        }
    }

    public boolean validateAndReload() {
        if (!runNginxTest()) {
            System.err.println("nginx -t failed, reload canceled !.");
            return false;
        }
        return reloadNginx();
    }

    private boolean runNginxTest() {
        return exec(new String[]{"nginx", "-t"}, "nginx -t");
    }

    private boolean reloadNginx() {
        return exec(new String[]{"nginx", "-s", "reload"}, "nginx -s reload");
    }

    private boolean exec(String[] cmd, String label) {
        try {
            ExecCreateCmdResponse execCreate = dockerClient.execCreateCmd(this.containerName)
                    .withCmd(cmd)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            StringBuilder output = new StringBuilder();
            dockerClient.execStartCmd(execCreate.getId())
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            output.append(new String(frame.getPayload()).trim());
                        }
                    })
                    .awaitCompletion(10, TimeUnit.SECONDS);

            InspectExecResponse inspect = dockerClient.inspectExecCmd(execCreate.getId()).exec();
            Long exitCode = inspect.getExitCodeLong();

            System.out.println("[%s] output: %s".formatted(label, output));

            if (exitCode == 0) {
                System.out.println("[%s] success".formatted(label));
                return true;
            } else {
                System.err.println("[%s] fail. Exit Code : %d".formatted(label, exitCode));
                return false;
            }

        } catch (Exception e) {
            System.err.println("[%s] fail: %s".formatted(label, e.getMessage()));
            return false;
        }
    }

}
