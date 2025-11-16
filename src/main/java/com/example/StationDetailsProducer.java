package com.example;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class StationDetailsProducer {

    @Inject
    @Channel("station-details-requests-out")
    Emitter<StationDetailsRequestMessage> stationDetailsRequestEmitter;

    public void requestStationDetails(Long trainStopId, String stationId) {
        Log.infof("Requesting station details for trainStop: %d, stationId: %s", trainStopId, stationId);
        stationDetailsRequestEmitter.send(new StationDetailsRequestMessage(trainStopId, stationId));
    }
}
