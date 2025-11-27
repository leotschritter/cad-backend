package de.htwg.travelwarnings.exception.exceptionhandler;


import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UniversalExceptionHandler
        implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(final Exception e) {
        return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity(e.getMessage())
                .build();
    }
}

