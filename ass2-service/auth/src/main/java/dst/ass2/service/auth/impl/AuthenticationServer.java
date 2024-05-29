package dst.ass2.service.auth.impl;

import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.IAuthenticationService;
import dst.ass2.service.api.auth.NoSuchUserException;
import dst.ass2.service.api.auth.proto.AuthenticationRequest;
import dst.ass2.service.api.auth.proto.AuthenticationResponse;
import dst.ass2.service.api.auth.proto.AuthServiceGrpc;
import dst.ass2.service.api.auth.proto.TokenValidationRequest;
import dst.ass2.service.api.auth.proto.TokenValidationResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

//Managed by the Spring container
//Settle up the gRPC server to make the authentication service available.
@ManagedBean
public class AuthenticationServer extends AuthServiceGrpc.AuthServiceImplBase {

    @Inject
    private IAuthenticationService authenticationService;

    // StreamObserver used for receiving streaming data in an asynchronous and non-blocking manner
    @Override
    public void authenticate(AuthenticationRequest request, StreamObserver<AuthenticationResponse> responseObserver) {
        AuthenticationResponse.Builder responseBuilder = AuthenticationResponse.newBuilder();
        try {
            // Call the IAuthenticationService (implemented from CachingAuthenticationService) to authenticate the user
            String token = authenticationService.authenticate(request.getEmail(), request.getPassword());
            responseBuilder.setToken(token).setAuthenticated(true);
        } catch (NoSuchUserException e) {
            handleAuthenticationError(responseObserver, Status.NOT_FOUND.withCause(e));
            return;
        } catch (AuthenticationException e) {
            handleAuthenticationError(responseObserver, Status.PERMISSION_DENIED.withCause(e));
            return;
        }

        //StreamObserver.onNext() is called to send a response to the client
        responseObserver.onNext(responseBuilder.build());

        // StreamObserver.onCompleted() is called to indicate that the client has finished sending messages
        responseObserver.onCompleted();
    }

    @Override
    public void validateToken(TokenValidationRequest request, StreamObserver<TokenValidationResponse> responseObserver) {
        boolean valid = authenticationService.isValid(request.getToken());
        TokenValidationResponse response = TokenValidationResponse.newBuilder().setValid(valid).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void handleAuthenticationError(StreamObserver<?> responseObserver, Status status) {
        //OnError() is called to send an error to the client
        responseObserver.onError(status.asException());
    }
}
