package dst.ass3.elastic.impl;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import dst.ass3.elastic.ContainerException;
import dst.ass3.elastic.ContainerInfo;
import dst.ass3.elastic.ContainerNotFoundException;
import dst.ass3.elastic.IContainerService;
import dst.ass3.messaging.Region;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerService implements IContainerService {

    // Docker client configuration
    private DockerClientConfig config;

    public ContainerService() {
        this.config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    // List all running Docker containers that are using the "dst/ass3-worker" image.
    @Override
    public List<ContainerInfo> listContainers() throws ContainerException {
        return executeWithDockerClient(docker -> {
            // Fetch all containers and filter those that are running the "dst/ass3-worker" image
            // Map each container to a ContainerInfo object and collect them into a list
            return docker.listContainersCmd().exec()
                    .stream()
                    .filter(c -> c.getImage().equals("dst/ass3-worker"))
                    .map(c -> {
                        var info = new ContainerInfo();
                        info.setContainerId(c.getId());
                        info.setImage(c.getImage());
                        info.setRunning(true);
                        info.setWorkerRegion(Region.valueOf(c.getCommand().split(" ")[2].toUpperCase()));
                        return info;
                    })
                    .collect(Collectors.toList());
        });
    }

    // Stops a running container with the given ID.
    @Override
    public void stopContainer(String containerId) throws ContainerException {
        executeWithDockerClient(docker -> {
            // Stop the container with the given ID
            docker.stopContainerCmd(containerId).exec();
            return null;
        });
    }

    //
    @Override
    public ContainerInfo startWorker(Region region) throws ContainerException {
        return executeWithDockerClient(docker -> {
            // Create a new container command with the specified image (docker build)
            var createContainerCommand = docker.createContainerCmd("dst/ass3-worker");

            // Configure the host for the container (auto-remove true --> remove container when stopped)
            var hostConfig = HostConfig.newHostConfig()
                    .withAutoRemove(true)
                    .withNetworkMode("dst");

            // Create container with host config, and set the region for the entrypoint script (i.e. docker run at_linz)
            createContainerCommand.withHostConfig(hostConfig)
                    .withCmd(region.toString().toLowerCase());

            // Execute the command to create the container (docker create)
            var container = createContainerCommand.exec();

            // Start the created container (docker start)
            docker.startContainerCmd(container.getId()).exec();

            // Create a new ContainerInfo object for the started container
            var info = new ContainerInfo();

            // Set the image of the container
            info.setImage("dst/ass3-worker");

            // Set the region of the worker
            info.setWorkerRegion(region);

            // Set the ID of the container
            info.setContainerId(container.getId());

            // Check if the container is running and set the running status in the ContainerInfo object
            var inspect = docker.inspectContainerCmd(container.getId()).exec();
            try {
                // Get running state and set it in the ContainerInfo object
                info.setRunning(inspect.getState().getRunning());
            } catch (NullPointerException e) {
                // if the state is null, the container is not running
                info.setRunning(false);
            }
            return info;
        });
    }


    // Helper to execute a Docker command.
    private <T> T executeWithDockerClient(DockerCommand<T> command) throws ContainerException {
        // Create docker client with given config and execute the command
        try (var docker = DockerClientBuilder.getInstance(config).build()) {
            return command.execute(docker);
        } catch (IOException e) {
            throw new ContainerException("Unable to connect to the docker instance");
        } catch (NotFoundException e) {
            throw new ContainerNotFoundException(e.getMessage());
        }
    }

    @FunctionalInterface
    private interface DockerCommand<T> {
        T execute(com.github.dockerjava.api.DockerClient docker) throws NotFoundException;
    }
}