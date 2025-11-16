package com.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Groupe of tests for proving the functionality of async creation and update of a TrainStop. For a POST /stops
 * - When a valid TrainStop with a stationId of {x} will return a 202 with the new TrainStop not including the station details of {x}
 * - As part of the request, a station-details-request for the stationId {x} will be sent out
 */
@QuarkusTest
public class TrainStopResourceAsyncTest {

    @Inject
    TrainStopResource trainStopResource;

    @ConfigProperty(name = "feature.toggle.station-details-async", defaultValue = "false")
    boolean stationDetailsAsync;

    @Test
    void testCreateTrainStop() {
        Assumptions.assumeTrue(stationDetailsAsync, "Station details async feature is disabled");

        // Given a valid trainStop:
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();

        // When the trainStop is create is called
        Response result = trainStopResource.create(trainStop);

        // Then:
        // - A 202 should be received
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Response.Status.ACCEPTED.getStatusCode(), result.getStatus());
        TrainStop resultTrainStop = (TrainStop) result.getEntity();
        Assertions.assertNotNull(resultTrainStop.id);
        Assertions.assertEquals(trainStop.stationId, resultTrainStop.stationId);
        // - the trainStop should be persisted in the database
        TrainStop persistedTrainStop = TrainStop.findById(trainStop.id);
        Assertions.assertNotNull(persistedTrainStop);
        Assertions.assertEquals(trainStop.stationId, persistedTrainStop.stationId);
        // - Note: a station-details-request will be sent out (tested elsewhere)
    }
}