package dst.ass2.service.trip.impl;

import dst.ass2.service.api.trip.DriverNotAvailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Runtime provider of the ExceptionMapper interface.
@Provider
public class DriverNotAvailableMapper implements ExceptionMapper<DriverNotAvailableException> {

    // Custom JAX-RS exception maps a DriverNotAvailableException to a HTTP 409 response.
    @Override
    public Response toResponse(DriverNotAvailableException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}