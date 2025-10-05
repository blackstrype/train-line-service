package com.example;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import java.time.Instant;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

@QuarkusTest
public class TrainStopResourceResilienceTest {

    @Inject
    TrainStopResource trainStopResource; // Inject the real service we are testing

    @InjectMock
    @RestClient
    StationService stationService;

    @Test
    void testRetryPolicy_SucceedsOnThirdAttempt() {
        // 1. Arrange: Program the mock's behavior for consecutive calls
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

        // 2. Act: Call the real service logic that uses the mock
        Response result = trainStopResource.create(trainStop);

        // 3. Assert: Verify we got the successful result after retries
        Assertions.assertNotNull(result);
        Assertions.assertEquals("1", ((TrainStop) result.getEntity()).stationId);

        // Also verify the mock was called exactly 3 times
        Mockito.verify(stationService, Mockito.times(3)).getStationById("1");
    }

    @Test
    void testCreateStop_WhenStationServiceIsSlow_ThrowsTimeoutException() {
        // 1. Arrange: Program the mock to simulate a 3-second delay.
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

        // 2. Act & Assert:
        // We now call our real service logic. We expect this call to fail
        // with a TimeoutException because its dependency (the mock) is too slow.
        // Assertions.assertThrows is the standard way to verify an exception is thrown.
        Assertions.assertThrows(TimeoutException.class, () -> {
            trainStopResource.create(trainStop);
        });

        // 3. Verify (Optional but good practice):
        // Confirm that our mock was indeed called exactly one time before it timed out.
        Mockito.verify(stationService, Mockito.times(1)).getStationById("1");
    }
    
    @Test
    void testCreateStop_WhenStationServiceIsSlowAndFailureProne_SucceedsOnThirdAttempt() {
        // 1. Arrange: Program the mock's behavior for consecutive calls
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

        // 2. Act: Call the real service logic that uses the mock
        Response result = trainStopResource.create(trainStop);

        // 3. Assert: Verify we got the successful result after retries
        Assertions.assertNotNull(result);
        Assertions.assertEquals("1", ((TrainStop) result.getEntity()).stationId);

        // Also verify the mock was called exactly 4 times
        Mockito.verify(stationService, Mockito.times(3)).getStationById("1");
    }

    @Test
    void testCircuitBreaker_OpenAfterConsecutiveFailures() {
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
        for(i = 0; i < 3; i++) {
            TrainStop trainStop = new TrainStop();
            trainStop.stationId = i + "";
            trainStop.arrivalTime = Instant.now();
            Response result = trainStopResource.create(trainStop);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(i + "", ((TrainStop) result.getEntity()).stationId);
        }
        // When and Then - Subsequent calls should immediately fail with CircuitBreakerOpenException
        TrainStop trainStop = new TrainStop();
        trainStop.stationId = i + "";
        trainStop.arrivalTime = Instant.now();
        Assertions.assertThrows(CircuitBreakerOpenException.class, () -> trainStopResource.create(trainStop));
        Assertions.assertThrows(CircuitBreakerOpenException.class, () -> trainStopResource.create(trainStop));

        // Verify that the stationService was called 10 times (3 requests*3 retries and 1 retry) to open the circuit
        Mockito.verify(stationService, Mockito.times(10)).getStationById(Mockito.anyString());
    }
}