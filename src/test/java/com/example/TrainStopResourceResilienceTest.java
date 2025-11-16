package com.example;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

@QuarkusTest
public class TrainStopResourceResilienceTest {

    @Inject
    TrainStopResource trainStopResource; // Inject the real service we are testing
    @Inject
    CircuitBreakerMaintenance circuitBreakerMaintenance;

    @ConfigProperty(name = "feature.toggle.station-details-async", defaultValue = "false")
    boolean stationDetailsAsync;

    @InjectMock
    @RestClient
    StationService stationService;

    @AfterEach
    public void resetCircuitBreaker() {
        circuitBreakerMaintenance.resetAll();
    }

    @Test
    void testRetryPolicy_SucceedsOnThirdAttempt() {
        Assumptions.assumeFalse(stationDetailsAsync, "Station details async feature is enabled");

        // Given: Program the mock's behavior for consecutive calls
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();

        Station mockStation = new Station();
        mockStation.id = "1";
        mockStation.name = "Success Station";

        Mockito.when(stationService.getStationById(Mockito.anyString()))
                .thenThrow(new WebApplicationException("First failure", 500))
                .thenThrow(new WebApplicationException("Second failure", 500))
                .thenReturn(mockStation);

        // When: Call the real service logic that uses the mock
        Response result = trainStopResource.create(trainStop);

        // Then: Verify we got the successful result after retries
        Assertions.assertNotNull(result);
        Assertions.assertEquals("1", ((TrainStop) result.getEntity()).stationId);

        // Also verify the mock was called exactly 3 times
        Mockito.verify(stationService, Mockito.times(3)).getStationById("1");
    }

    @Test
    void testCreateStop_WhenStationServiceIsSlow_UsesFallback() {
        Assumptions.assumeFalse(stationDetailsAsync, "Station details async feature is enabled");

        // Given: Program the mock to simulate a 3-second delay.
        // This is intentionally longer than the @Timeout(2000) on the client method.
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();

        Station mockStation = new Station();
        mockStation.id = "1";
        mockStation.name = "Success Station";

        Mockito.doAnswer(invocation -> {
            Thread.sleep(6000);
            // This return is never reached because the timeout will interrupt it.
            return mockStation;
        }).when(stationService).getStationById(Mockito.anyString());

        // When: create is called and will provide a result using the fallback station
        Response result = trainStopResource.create(trainStop);

        // Then: the create should nonetheless succeed with a 201
        Assertions.assertNotNull(result);
        Assertions.assertEquals("1", ((TrainStop) result.getEntity()).stationId);

        // Confirm that our mock was indeed called exactly one time before it timed out.
        Mockito.verify(stationService, Mockito.times(1)).getStationById("1");
    }
    
    @Test
    void testCreateStop_WhenStationServiceIsSlowAndFailureProne_SucceedsOnThirdAttempt() {
        Assumptions.assumeFalse(stationDetailsAsync, "Station details async feature is enabled");

        // Given: Program the mock's behavior for consecutive calls
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();

        Station mockStation = new Station();
        mockStation.id = "1";
        mockStation.name = "Success Station";

        Mockito.when(stationService.getStationById(Mockito.anyString()))
                .thenAnswer(invocation -> {
                    Thread.sleep(950); // Simulate delay
                    throw new WebApplicationException("First failure", 500);
                })
                .thenAnswer(invocation -> {
                    Thread.sleep(950); // Simulate delay
                    throw new WebApplicationException("Second failure", 500);
                })
                .thenAnswer(invocation -> {
                    Thread.sleep(950); // Simulate delay
                    return mockStation;
                });

        // When: Call the real service logic that uses the mock
        Response result = trainStopResource.create(trainStop);

        // Then: Verify we got the successful result after retries
        Assertions.assertNotNull(result);
        Assertions.assertEquals("1", ((TrainStop) result.getEntity()).stationId);

        // Also verify the mock was called exactly 4 times
        Mockito.verify(stationService, Mockito.times(3)).getStationById("1");
    }

    @Test
    void testCircuitBreaker_OpenAfterConsecutiveFailures() {
        Assumptions.assumeFalse(stationDetailsAsync, "Station details async feature is enabled");

        // Given: Program the mock to fail consecutively
        // Simulate 6 consecutive failed requests to open the circuit
        // Each call to create() will attempt 3 retries, so we need 6 / 3 = 2 failed create requests to open the circuit
        Mockito.when(stationService.getStationById(Mockito.anyString()))
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenReturn(new Station())
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenReturn(new Station())
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenReturn(new Station())
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenReturn(new Station()); // This should not be called if circuit is open

        // When - First calls with 3 retries should succeed open the circuit
        int i;
        for (i = 0; i < 3; i++) {
            TrainStop trainStop = new TrainStop();
            trainStop.stationId = i + "";
            trainStop.arrivalTime = Instant.now();
            Response result = trainStopResource.create(trainStop);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(i + "", ((TrainStop) result.getEntity()).stationId);
        }
        // When and Then - Subsequent calls should succeed via the fallback
        for (; i < 6; i++) {
            TrainStop trainStop = new TrainStop();
            trainStop.stationId = i + "";
            trainStop.arrivalTime = Instant.now();
            Response result = trainStopResource.create(trainStop);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(i + "", ((TrainStop) result.getEntity()).stationId);
        }

        // Verify that the stationService was called 10 times (3 requests*3 retries and 1 retry) to open the circuit and thereafter the fallback was used
        Mockito.verify(stationService, Mockito.times(10)).getStationById(Mockito.anyString());
    }

    @Test
    void testFallback_ProvidesDefaultWhenCircuitIsOpen() {
        Assumptions.assumeFalse(stationDetailsAsync, "Station details async feature is enabled");

        // Given: Program the mock to fail consecutively and to open the circuit breaker
        Mockito.when(stationService.getStationById(Mockito.anyString()))
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenThrow(new WebApplicationException("Failure", 500))
                .thenThrow(new WebApplicationException("Failure", 500));
        
        // When: StationService station details are requested and the three retries fail
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = "1";
        trainStop.arrivalTime = Instant.now();
        // These calls will trigger the max retry failure and a fallback Station should be provided
        Response result = trainStopResource.create(trainStop);

        // Then: The fallback station should be provided and prevent exceptions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());
        TrainStop createdTrainStop = (TrainStop) result.getEntity();
        Assertions.assertEquals("1", createdTrainStop.stationId); // Assert fallback value

        // Verify that stationService was called enough times to trigger a fallback
        Mockito.verify(stationService, Mockito.times(4)).getStationById(Mockito.anyString());
    }
}