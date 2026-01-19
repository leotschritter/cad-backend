package de.htwg.tenant.service;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.firestore.v1.FirestoreAdminClient;
import com.google.firestore.admin.v1.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Service for managing Firestore databases via Admin API.
 */
@ApplicationScoped
public class FirestoreService {

    private static final Logger LOG = Logger.getLogger(FirestoreService.class);

    @ConfigProperty(name = "gcp.project-id")
    String projectId;

    /**
     * Create a Firestore database for a tenant.
     * 
     * @param databaseId Database ID (e.g., tenant ID)
     * @return The created database name
     */
    public String createDatabase(String databaseId) {
        try (FirestoreAdminClient firestoreAdmin = FirestoreAdminClient.create()) {
            
            String parent = String.format("projects/%s", projectId);
            
            // Check if database already exists
            try {
                String databaseName = String.format("projects/%s/databases/%s", projectId, databaseId);
                Database existingDb = firestoreAdmin.getDatabase(databaseName);
                LOG.infof("Firestore database already exists: %s", databaseId);
                return existingDb.getName();
            } catch (NotFoundException e) {
                // Database doesn't exist, create it
                LOG.infof("Creating Firestore database: %s", databaseId);
            }

            // Create database
            Database database = Database.newBuilder()
                .setType(Database.DatabaseType.FIRESTORE_NATIVE)
                .setLocationId("europe-west1")
                .setConcurrencyMode(Database.ConcurrencyMode.OPTIMISTIC)
                .setAppEngineIntegrationMode(Database.AppEngineIntegrationMode.DISABLED)
                .build();

            CreateDatabaseRequest request = CreateDatabaseRequest.newBuilder()
                .setParent(parent)
                .setDatabase(database)
                .setDatabaseId(databaseId)
                .build();

            // This is a long-running operation
            Database createdDatabase = firestoreAdmin.createDatabaseAsync(request).get();
            
            LOG.infof("✅ Firestore database created: %s", createdDatabase.getName());
            return createdDatabase.getName();

        } catch (NotFoundException e) {
            LOG.errorf(e, "❌ Project not found: %s", projectId);
            throw new RuntimeException("Failed to create Firestore database: project not found", e);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to create Firestore database: %s", databaseId);
            throw new RuntimeException("Failed to create Firestore database", e);
        }
    }

    /**
     * Delete a Firestore database.
     * 
     * @param databaseId Database ID to delete
     */
    public void deleteDatabase(String databaseId) {
        try (FirestoreAdminClient firestoreAdmin = FirestoreAdminClient.create()) {
            
            String databaseName = String.format("projects/%s/databases/%s", projectId, databaseId);
            
            LOG.infof("Deleting Firestore database: %s", databaseId);

            DeleteDatabaseRequest request = DeleteDatabaseRequest.newBuilder()
                .setName(databaseName)
                .build();

            // This is a long-running operation
            firestoreAdmin.deleteDatabaseAsync(request).get();
            
            LOG.infof("✅ Firestore database deleted: %s", databaseId);

        } catch (NotFoundException e) {
            LOG.warnf("Firestore database not found (may already be deleted): %s", databaseId);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to delete Firestore database: %s", databaseId);
            // Don't throw, just log - database might not exist
        }
    }

    /**
     * Check if a Firestore database exists.
     * 
     * @param databaseId Database ID to check
     * @return true if database exists
     */
    public boolean databaseExists(String databaseId) {
        try (FirestoreAdminClient firestoreAdmin = FirestoreAdminClient.create()) {
            
            String databaseName = String.format("projects/%s/databases/%s", projectId, databaseId);
            
            firestoreAdmin.getDatabase(databaseName);
            return true;

        } catch (NotFoundException e) {
            return false;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error checking if database exists: %s", databaseId);
            return false;
        }
    }

}
