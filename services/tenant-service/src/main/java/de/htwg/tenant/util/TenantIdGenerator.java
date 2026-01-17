package de.htwg.tenant.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for generating tenant IDs from tenant names.
 */
public class TenantIdGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    /**
     * Generate a URL-safe tenant ID from a tenant name.
     * Example: "Acme Corp!" -> "acme-corp"
     */
    public static String generateTenantId(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }

        // Normalize to NFD (decompose accented characters)
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);

        // Convert to lowercase
        String lowercase = normalized.toLowerCase(Locale.ROOT);

        // Replace whitespace with hyphens
        String noWhitespace = WHITESPACE.matcher(lowercase).replaceAll("-");

        // Remove non-latin characters (keep alphanumeric and hyphens)
        String latin = NON_LATIN.matcher(noWhitespace).replaceAll("");

        // Replace multiple consecutive hyphens with single hyphen
        String singleHyphens = MULTIPLE_HYPHENS.matcher(latin).replaceAll("-");

        // Remove leading/trailing hyphens
        String trimmed = singleHyphens.replaceAll("^-+|-+$", "");

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Tenant name must contain at least one alphanumeric character");
        }

        return trimmed;
    }

    /**
     * Generate namespace name for Kubernetes.
     */
    public static String generateNamespace(String tenantId) {
        return "tenant-" + tenantId;
    }

    /**
     * Generate subdomain for the tenant.
     */
    public static String generateSubdomain(String tenantId, String baseDomain) {
        return tenantId + "." + baseDomain;
    }
}

