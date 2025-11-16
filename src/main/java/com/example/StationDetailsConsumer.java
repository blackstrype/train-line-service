package com.example;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Optional;

@ApplicationScoped
public class StationDetailsConsumer {

    @Inject
    @RestClient
    StationService stationService;

    @Incoming("station-details-requests-in")
    @Transactional
    @Retry(maxRetries = 3, delay = 1000)
    public void processStationDetailsRequest(StationDetailsRequestMessage request) {
        Log.infof("Received station detail request for trainStop: %d, stationId: %s", request.trainStopId(), request.stationId());

        Optional<TrainStop> stopOptional = TrainStop.findByIdOptional(request.trainStopId());
        if (stopOptional.isEmpty()) {
            Log.warnf("No TrainStop with id %d, skipping update.", request.trainStopId());
            return;
        }
        TrainStop stopToUpdate = stopOptional.get();

        try {
            Station station = stationService.getStationById(request.stationId());
            Log.infof("Successfully fetched details for station: %s", station.name);
            stopToUpdate.stationName = station.name;
            stopToUpdate.persist();
        } catch (WebApplicationException e) {
            if(e.getResponse().getStatus() == 404) {
                Log.errorf(e, "StationId %s does not exist. Deleting TrainStop %d", request.stationId(), request.trainStopId());
                stopToUpdate.delete();
            } else {
                Log.errorf(e, "Failed to fetch details for station %s. Updating TrainStop with arbitrary station details", request.stationId());
                stopToUpdate.stationName = "Station details not available";
                stopToUpdate.persist();
            }
        }
    }
}
