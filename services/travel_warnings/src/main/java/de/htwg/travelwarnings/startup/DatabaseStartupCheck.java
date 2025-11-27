package de.htwg.travelwarnings.startup;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Startup verification bean to check database connectivity and schema
 */
@ApplicationScoped
public class DatabaseStartupCheck {

    private static final Logger LOG = Logger.getLogger(DatabaseStartupCheck.class);

    @Inject
    DataSource dataSource;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("=== Database Startup Check ===");
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            LOG.infof("Connected to: %s %s", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
            LOG.infof("Database URL: %s", metaData.getURL());
            LOG.infof("Database User: %s", metaData.getUserName());
            
            // Check if our tables exist
            checkTable(metaData, "travel_warnings");
            checkTable(metaData, "user_trips");
            checkTable(metaData, "warning_notifications");
            
            LOG.info("=== Database Check Complete ===");
            
        } catch (Exception e) {
            LOG.error("❌ DATABASE CONNECTION FAILED!", e);
            LOG.error("Application will not work without database connection!");
            LOG.error("Please ensure PostgreSQL is running: docker ps | findstr travel-warnings-db");
        }
    }

    private void checkTable(DatabaseMetaData metaData, String tableName) throws Exception {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                LOG.infof("✅ Table '%s' exists", tableName);
                
                // Count indexes
                try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, false, false)) {
                    int indexCount = 0;
                    while (indexes.next()) {
                        indexCount++;
                    }
                    if (indexCount > 0) {
                        LOG.debugf("   - Has %d indexes", indexCount);
                    }
                }
            } else {
                LOG.warnf("⚠️  Table '%s' does NOT exist (Hibernate should create it)", tableName);
            }
        }
    }
}

