package de.htwg.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Manual CORS filter to ensure CORS headers are always added.
 * Required because Quarkus CORS configuration in properties/yaml sometimes doesn't work reliably.
 */
@Provider
public class CorsConfigurationFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        
        String origin = requestContext.getHeaderString("Origin");
        
        // Allow requests from localhost origins (development)
        if (origin != null && (origin.contains("localhost") || origin.contains("127.0.0.1"))) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
            responseContext.getHeaders().add("Access-Control-Allow-Methods", 
                "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
            responseContext.getHeaders().add("Access-Control-Allow-Headers", 
                "Origin, Content-Type, Accept, Authorization, X-Requested-With, Cache-Control, If-Modified-Since");
            responseContext.getHeaders().add("Access-Control-Expose-Headers", 
                "Location, Content-Disposition");
            responseContext.getHeaders().add("Access-Control-Max-Age", "86400");
        }
    }
}

