package dst.ass2.service.auth.client.impl;

import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.NoSuchUserException;
import dst.ass2.service.api.auth.proto.*;
import dst.ass2.service.auth.client.AuthenticationClientProperties;
import dst.ass2.service.auth.client.IAuthenticationClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class GrpcAuthenticationClient implements IAuthenticationClient {

    // The channel is the abstraction to connect to a service endpoint
    private final ManagedChannel channel;

    // Client side stub/client side proxy which contains the methods for each function defined in the service
    // It's a blocking/synchronous stub, it sends the request and waits for the response
    private final AuthServiceGrpc.AuthServiceBlockingStub blockingStub;

    public GrpcAuthenticationClient(AuthenticationClientProperties properties) {
        channel = ManagedChannelBuilder.forAddress(properties.getHost(), properties.getPort())
                .usePlaintext()
                .build();
        blockingStub = AuthServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public String authenticate(String email, String password) throws NoSuchUserException, AuthenticationException {
        AuthenticationRequest request = AuthenticationRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();
        try {
            AuthenticationResponse response = blockingStub.authenticate(request);
            return response.getToken();
        } catch (StatusRuntimeException e) {
            handleStatusRuntimeException(e);
            return null;
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        TokenValidationRequest request = TokenValidationRequest.newBuilder()
                .setToken(token)
                .build();
        TokenValidationResponse response = blockingStub.validateToken(request);
        return response.getValid();
    }

    @Override
    public void close() {
        channel.shutdown();
    }

    private void handleStatusRuntimeException(StatusRuntimeException e) throws NoSuchUserException, AuthenticationException {
        Status status = Status.fromThrowable(e);
        if (status.getCode() == Status.Code.NOT_FOUND) {
            throw new NoSuchUserException();
        } else if (status.getCode() == Status.Code.PERMISSION_DENIED) {
            throw new AuthenticationException();
        } else {
            throw new RuntimeException(e);
        }
    }
}
