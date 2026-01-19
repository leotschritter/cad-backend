package de.htwg.tenant.service;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.firestore.v1.FirestoreAdminClient;
import com.google.firestore.admin.v1.*;
import com.google.longrunning.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Service for managing Firestore databases and indexes via Admin API.
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

    /**
     * Create required Firestore indexes for comments-likes service.
     * Creates composite indexes needed for queries in the comments and likes collections.
     * 
     * @param databaseId Database ID (e.g., tenant ID or "(default)")
     */
    public void createRequiredIndexes(String databaseId) {
        try (FirestoreAdminClient firestoreAdmin = FirestoreAdminClient.create()) {
            
            String parent = String.format("projects/%s/databases/%s", projectId, databaseId);
            
            LOG.infof("📊 Creating required Firestore indexes for database: %s", databaseId);
            
            // Index 1: Likes collection - userEmail + createdAt (for getLikesByUser)
            createCompositeIndex(firestoreAdmin, parent, "likes", 
                Index.IndexField.newBuilder()
                    .setFieldPath("userEmail")
                    .setOrder(Index.IndexField.Order.ASCENDING)
                    .build(),
                Index.IndexField.newBuilder()
                    .setFieldPath("createdAt")
                    .setOrder(Index.IndexField.Order.DESCENDING)
                    .build()
            );
            
            // Index 2: Comments collection - userEmail + createdAt (for getCommentsByUser)
            createCompositeIndex(firestoreAdmin, parent, "comments",
                Index.IndexField.newBuilder()
                    .setFieldPath("userEmail")
                    .setOrder(Index.IndexField.Order.ASCENDING)
                    .build(),
                Index.IndexField.newBuilder()
                    .setFieldPath("createdAt")
                    .setOrder(Index.IndexField.Order.DESCENDING)
                    .build()
            );
            
            // Index 3: Comments collection - itineraryId + createdAt (for getCommentsForItinerary)
            createCompositeIndex(firestoreAdmin, parent, "comments",
                Index.IndexField.newBuilder()
                    .setFieldPath("itineraryId")
                    .setOrder(Index.IndexField.Order.ASCENDING)
                    .build(),
                Index.IndexField.newBuilder()
                    .setFieldPath("createdAt")
                    .setOrder(Index.IndexField.Order.DESCENDING)
                    .build()
            );
            
            LOG.infof("✅ All required Firestore indexes created/verified for database: %s", databaseId);
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to create Firestore indexes for database: %s", databaseId);
            // Don't throw - indexes might already exist or be created later
            // The service will work once indexes are built
        }
    }

    /**
     * Create a composite index for a collection group.
     * If the index already exists, this method will skip creation.
     * 
     * @param firestoreAdmin Firestore Admin client
     * @param parent Parent path (projects/{project}/databases/{database})
     * @param collectionGroup Collection group name (e.g., "likes", "comments")
     * @param fields Index fields
     */
    private void createCompositeIndex(FirestoreAdminClient firestoreAdmin, String parent,
                                     String collectionGroup, Index.IndexField... fields) {
        try {
            // Build the index
            Index index = Index.newBuilder()
                .setQueryScope(Index.QueryScope.COLLECTION)
                .addAllFields(java.util.Arrays.asList(fields))
                .build();

            // Check if index already exists by listing existing indexes
            try {
                String collectionGroupPath = parent + "/collectionGroups/" + collectionGroup;
                boolean indexExists = false;
                for (Index existingIndex : firestoreAdmin.listIndexes(collectionGroupPath).iterateAll()) {
                    if (indexesMatch(existingIndex, index)) {
                        indexExists = true;
                        break;
                    }
                }
                if (indexExists) {
                    LOG.debugf("Index already exists for collection '%s' with fields: %s (skipping creation)",
                        collectionGroup, getIndexFieldsDescription(fields));
                    return;
                }
            } catch (Exception e) {
                // If we can't check, proceed with creation - it will handle "already exists" error
                LOG.debugf("Could not check if index exists for collection '%s', proceeding with creation: %s",
                    collectionGroup, e.getMessage());
            }

            // Create the index request
            CreateIndexRequest request = CreateIndexRequest.newBuilder()
                .setParent(parent)
                .setIndex(index)
                .build();

            // Create the index (this is a long-running operation)
            LOG.infof("Creating index for collection '%s' with fields: %s",
                collectionGroup, getIndexFieldsDescription(fields));

            // Start the index creation operation
            // createIndexCallable returns a UnaryCallable that we can call to get an Operation
            Operation operation = firestoreAdmin.createIndexCallable().call(request);

            // The operation is asynchronous - it will build in the background
            // We don't wait for completion as it can take several minutes
            LOG.infof("✅ Index creation started for collection '%s'. " +
                "Index will be built in the background (may take a few minutes).", collectionGroup);

        } catch (com.google.api.gax.rpc.AlreadyExistsException e) {
            LOG.debugf("Index already exists for collection '%s' (this is OK)", collectionGroup);
        } catch (Exception e) {
            // Check if it's an "already exists" error
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                LOG.debugf("Index already exists for collection '%s' (this is OK)", collectionGroup);
            } else {
                LOG.warnf(e, "⚠️ Could not create index for collection '%s'. " +
                    "Index may already exist or will be created automatically when first used. Error: %s",
                    collectionGroup, e.getMessage());
            }
        }
    }

    /**
     * Check if two indexes match (same fields and order).
     */
    private boolean indexesMatch(Index existing, Index newIndex) {
        if (existing.getFieldsCount() != newIndex.getFieldsCount()) {
            return false;
        }
        if (existing.getQueryScope() != newIndex.getQueryScope()) {
            return false;
        }
        for (int i = 0; i < existing.getFieldsCount(); i++) {
            Index.IndexField existingField = existing.getFields(i);
            Index.IndexField newField = newIndex.getFields(i);
            if (!existingField.getFieldPath().equals(newField.getFieldPath()) ||
                existingField.getOrder() != newField.getOrder()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a human-readable description of index fields.
     */
    private String getIndexFieldsDescription(Index.IndexField... fields) {
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) desc.append(", ");
            desc.append(fields[i].getFieldPath())
                .append(" ")
                .append(fields[i].getOrder() == Index.IndexField.Order.ASCENDING ? "ASC" : "DESC");
        }
        return desc.toString();
    }
}
