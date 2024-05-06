package dst.ass2.service.auth.grpc.impl;

import dst.ass2.service.auth.grpc.GrpcServerProperties;
import dst.ass2.service.auth.grpc.IGrpcServerRunner;
import dst.ass2.service.auth.impl.AuthenticationServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
public class GrpcServerRunner implements IGrpcServerRunner {
    private static final Logger logger = Logger.getLogger(GrpcServerRunner.class.getName());

    @Inject
    private GrpcServerProperties grpcServerProperties;

    @Inject
    private AuthenticationServer authenticationServer;

    @Override
    public void run() throws IOException {
        Server server = buildServer();
        startServer(server);
        logger.info("gRPC server started, listening on port " + grpcServerProperties.getPort());
        addShutdownHook(server);
    }

    private Server buildServer() {
        return ServerBuilder.forPort(grpcServerProperties.getPort())
                .addService(authenticationServer)
                .build();
    }

    private void startServer(Server server) throws IOException {
        server.start();
    }

    private void addShutdownHook(Server server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server...");
            server.shutdown();
            logger.info("gRPC server shut down.");
        }));
    }
}
