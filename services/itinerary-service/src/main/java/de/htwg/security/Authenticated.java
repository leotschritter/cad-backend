package de.htwg.security;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark REST endpoints that require Google Cloud Identity Platform authentication.
 * Apply this annotation to methods or classes that should only be accessible
 * to authenticated users.
 * Identity Platform is Google's CIAM (Customer Identity and Access Management) solution
 * providing authentication as a service with enterprise features.
 * Example usage:
 * <pre>
 * {@code
 * @Authenticated
 * @GET
 * @Path("/protected")
 * public Response getProtectedResource() {
 *     // Only authenticated users can access this endpoint
 *     return Response.ok().build();
 * }
 * }
 * </pre>
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Authenticated {
}

