package de.htwg.travelwarnings.scheduler;

import de.htwg.travelwarnings.service.AlertDispatcherService;
import de.htwg.travelwarnings.service.TravelWarningFetcherService;
import de.htwg.travelwarnings.service.WarningMatcherService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Scheduled job to poll travel warnings from Auswärtiges Amt API
 * and dispatch alerts to affected users.
 *
 * Implements the automated polling and notification flow from the architecture.
 */
@ApplicationScoped
public class TravelWarningScheduler {

    private static final Logger LOG = Logger.getLogger(TravelWarningScheduler.class);

    @Inject
    TravelWarningFetcherService fetcherService;

    @Inject
    WarningMatcherService matcherService;

    @Inject
    AlertDispatcherService alertService;

    /**
     * Poll travel warnings every 15 minutes (configurable via application.properties)
     * Process flow:
     * 1. Fetch latest warnings from API
     * 2. Match warnings with user trips
     * 3. Dispatch alerts to affected users
     */
    @Scheduled(cron = "{travel-warnings.poll.cron}")
    @Transactional
    public void pollAndNotify() {
        LOG.info("Starting scheduled travel warning poll and notification job");

        try {
            // Step 1: Fetch and update warnings
            int updatedWarnings = fetcherService.fetchAndUpdateAllWarnings();
            LOG.infof("Fetched and updated %d travel warnings", updatedWarnings);

            if (updatedWarnings == 0) {
                LOG.info("No warnings updated, skipping notification step");
                return;
            }

            // Step 2: Find all warning-trip matches
            List<WarningMatcherService.WarningMatch> matches = matcherService.findAllActiveWarningMatches();
            LOG.infof("Found %d warning-trip matches to process", matches.size());

            // Step 3: Send alerts (only sends if not already sent for this version)
            int alertsSent = 0;
            for (WarningMatcherService.WarningMatch match : matches) {
                try {
                    // Use the email directly from the trip
                    String userEmail = match.getTrip().getEmail();

                    boolean sent = alertService.sendAlert(
                        match.getWarning(),
                        match.getTrip(),
                        userEmail
                    );

                    if (sent) {
                        alertsSent++;
                    }
                } catch (Exception e) {
                    LOG.errorf(e, "Error sending alert for trip %d", match.getTrip().getId());
                }
            }

            LOG.infof("Successfully sent %d travel warning alerts", alertsSent);

        } catch (Exception e) {
            LOG.error("Error in travel warning poll and notification job", e);
        }
    }

    /**
     * Manual trigger for testing (can be called via management endpoint)
     */
    public void triggerManualPoll() {
        LOG.info("Manual poll triggered");
        pollAndNotify();
    }
}

