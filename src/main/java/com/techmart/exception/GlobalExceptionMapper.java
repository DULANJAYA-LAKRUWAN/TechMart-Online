package com.techmart.exception;

import com.techmart.dto.ApiResponseDTO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAX-RS @Provider that maps TechMart exceptions to appropriate HTTP responses.
 * Provides consistent error envelope format for all REST endpoints.
 *
 * Mapping:
 *   ProductNotFoundException       → 404 Not Found
 *   InsufficientInventoryException → 409 Conflict
 *   OrderProcessingException       → 422 Unprocessable Entity
 *   TechMartException              → 400 Bad Request
 *   Exception (catch-all)          → 500 Internal Server Error
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        LOG.log(Level.WARNING, "Exception caught by GlobalExceptionMapper", exception);

        if (exception instanceof ProductNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiResponseDTO.error(exception.getMessage(),
                    ((TechMartException) exception).getErrorCode()))
                .build();
        }

        if (exception instanceof InsufficientInventoryException) {
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiResponseDTO.error(exception.getMessage(),
                    ((TechMartException) exception).getErrorCode()))
                .build();
        }

        if (exception instanceof OrderProcessingException) {
            return Response.status(422) // Unprocessable Entity
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiResponseDTO.error(exception.getMessage(),
                    ((TechMartException) exception).getErrorCode()))
                .build();
        }

        if (exception instanceof TechMartException) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiResponseDTO.error(exception.getMessage(),
                    ((TechMartException) exception).getErrorCode()))
                .build();
        }

        // Catch-all: 500 Internal Server Error — hide internals from client
        LOG.log(Level.SEVERE, "Unhandled exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(ApiResponseDTO.error("An internal error occurred. Please try again later.",
                "INTERNAL_ERROR"))
            .build();
    }
}
