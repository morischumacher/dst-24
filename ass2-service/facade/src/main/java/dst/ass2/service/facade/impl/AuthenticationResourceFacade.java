package dst.ass2.service.facade.impl;

import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.NoSuchUserException;
import dst.ass2.service.api.auth.rest.IAuthenticationResource;
import dst.ass2.service.auth.client.IAuthenticationClient;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;


// The facade pattern is used here to abstract the details of making HTTP requests to the IAuthenticationResource.
// This allows the rest of the application to use AuthenticationResourceFacade without needing to know about
// the details of HTTP requests, URIs.

//gRPC client is used
@Path("/auth")
public class AuthenticationResourceFacade implements IAuthenticationResource {

    private final IAuthenticationClient client;

    @Inject
    public AuthenticationResourceFacade(IAuthenticationClient client) {
        this.client = client;
    }

    @Override
    @POST
    @Path("authenticate")
    public Response authenticate(@FormParam("email") String email, @FormParam("password") String password) throws NoSuchUserException, AuthenticationException {
        String token = client.authenticate(email, password);
        return Response.ok(token).build();
    }
}
