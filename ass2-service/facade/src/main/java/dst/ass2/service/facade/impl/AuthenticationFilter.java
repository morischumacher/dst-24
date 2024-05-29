package dst.ass2.service.facade.impl;

import dst.ass2.service.auth.client.IAuthenticationClient;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

// Automatically discovered and applied to all resources
@Provider
// Only applied to resources and methods annotated with @AuthenticationRequired
@AuthenticationRequired
public class AuthenticationFilter implements ContainerRequestFilter {

    @Inject
    private IAuthenticationClient authClient;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String token = extractToken(requestContext);
        System.out.println("AUTHFILTER, token:" + token);

        validateToken(token);
    }

    private String extractToken(ContainerRequestContext requestContext) {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new NotAuthorizedException("Bearer");
        }
        return authorizationHeader.substring("Bearer ".length());
    }

    private void validateToken(String token) {
        System.out.println("AUTHFILTER, trimmed token:" + token);
        if (!authClient.isTokenValid(token)) {
            throw new NotAuthorizedException("Bearer error=\"invalid_token\"");
        }
    }
}
