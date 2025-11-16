package com.example;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Group of tests for proving the functionality of async creation and update of a TrainStop as part of the
 * TrainStop creation SAGA. When a message is present on the station-details-request channel, the StationDetailsConsumer will handle the message by:
 * - Acknowledge the message (this could be done automatically)
 * - For a message with stationId {s} and TrainStop {t}
 *  - If the TrainStop {t} doesn't exist Log an error and exit
 *  - Else send a request to station-service for details of station {s}
 *    - If the stationDetails request is successful use the stationDetails to update {t}
 *    - If the stationDetails request is unsuccessful
 *      - If the error is a 404, station {s} doesn't exist in the database
 *        - delete {t} from the database
 *        - Log a corresponding error
 *      - else station {s} may exist in the database but the request failed
 *        - Update the TrainStop {t} stationName with "Station details not available"
 *        - Log a corresponding error
 */
@QuarkusTest
public class StationDetailsConsumerTest {

    @InjectMock
    @RestClient
    StationService stationService;

    @Inject
    StationDetailsConsumer stationDetailsConsumer;

    @ConfigProperty(name = "feature.toggle.station-details-async")
    boolean stationDetailsAsync;

    private static final Logger stationDetailsConsumerLogger = Logger.getLogger(StationDetailsConsumer.class.getCanonicalName());
    private InMemoryLogHandler logHandler;

    @BeforeEach
    @Transactional
    void setup() {
        logHandler = new InMemoryLogHandler();
        logHandler.setLevel(Level.ALL);
        stationDetailsConsumerLogger.addHandler(logHandler);
        TrainStop.deleteAll();
    }

    @AfterEach
    void teardown() {
        stationDetailsConsumerLogger.removeHandler(logHandler);
        logHandler.clear();
    }

    @Test
    @Transactional
    void testConsumerSuccess() {
        Assumptions.assumeTrue(stationDetailsAsync, "Station details async feature is disabled");

        // Given
        // The TrainStop with the StationId exists in the database
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();
        trainStop.persist();
        // Station details exist and the station service will succeed
        Station station = new Station();
        station.id = "1";
        station.name = "Station 1";
        station.location = "Location 1";
        Mockito.when(stationService.getStationById("1")).thenReturn(station);

        // When a message is sent
        var stationDetailsRequest = new StationDetailsRequestMessage(trainStop.id, trainStop.stationId);
        stationDetailsConsumer.processStationDetailsRequest(stationDetailsRequest);

        // Verify that the StationDetailsConsumer processed the message and updated the TrainStop
//        Assertions.assertEquals(1, logHandler.getMessages().size());
//        Assertions.assertEquals(String.format("Successfully fetched details for station: %s", station.name), logHandler.getMessages().getFirst());
        TrainStop updatedTrainStop = TrainStop.findById(trainStop.id);
        Assertions.assertEquals(station.name, updatedTrainStop.stationName);
    }

    @Test
    @Transactional
    void testConsumerTrainStopNotFound() {
        Assumptions.assumeTrue(stationDetailsAsync, "Station details async feature is disabled");

        // Given
        // TrainStop does not exist in the database
        Long trainStopId = 1L;

        // When a message is sent
        var stationDetailsRequest = new StationDetailsRequestMessage(trainStopId, "1");
        stationDetailsConsumer.processStationDetailsRequest(stationDetailsRequest);

        // Verify that the StationDetailsConsumer error was Logged indicating that the TrainStop was not found
//        Assertions.assertEquals(1, logHandler.getMessages().size());
//        Assertions.assertEquals(String.format("TrainStop %d not found", trainStopId), logHandler.getMessages().getFirst());
    }

    @Test
    @Transactional
    void testConsumerTrainStopFoundStationNotFound() {
        Assumptions.assumeTrue(stationDetailsAsync, "Station details async feature is disabled");

        // Given
        // The TrainStop with the StationId exists in the database
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();
        trainStop.persist();

        // When a message is sent
        Mockito.when(stationService.getStationById("1")).thenThrow(new WebApplicationException("Station not found", 404));
        var stationDetailsRequest = new StationDetailsRequestMessage(trainStop.id, trainStop.stationId);
        stationDetailsConsumer.processStationDetailsRequest(stationDetailsRequest);

        // Verify that the StationDetailsConsumer error was logged indicating that the Station details were not found
//        Assertions.assertEquals(1, logHandler.getMessages().size());
//        Assertions.assertEquals(
//                String.format("StationId %s for TrainStop %d does not exist. Deleting TrainStop %d", trainStop.stationId, trainStop.id, trainStop.id),
//                logHandler.getMessages().getFirst());
        // Verify that the trainStop was removed from the database
        Assertions.assertNull(TrainStop.findById(trainStop.id));
    }

    @Test
    @Transactional
    void testConsumerTrainStopFoundStationRequestFailed() {
        Assumptions.assumeTrue(stationDetailsAsync, "Station details async feature is disabled");

        // Given
        // The TrainStop with the StationId exists in the database
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();
        trainStop.persist();
        var stationDetailsRequest = new StationDetailsRequestMessage(trainStop.id, trainStop.stationId);

        // When
        // the station-service request fails
        Mockito.when(stationService.getStationById("1")).thenThrow(new WebApplicationException("Station not found", 500));
        // a message is sent
        stationDetailsConsumer.processStationDetailsRequest(stationDetailsRequest);

        // Verify that the StationDetailsConsumer error was logged indicating that the Station details were not found
//        Assertions.assertEquals(1, logHandler.getMessages().size());
//        Assertions.assertContains(
//                String.format("The request failed for StationId %s failed. Updating TrainStop %d with arbitrary station details", trainStop.stationId, trainStop.id),
//                logHandler.getMessages().getFirst());
        TrainStop updatedTrainStop = TrainStop.findById(trainStop.id);
        Assertions.assertEquals("Station details not available", updatedTrainStop.stationName);
    }
}