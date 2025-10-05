package com.example;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@RegisterRestClient(configKey = "station-service")
@Path("/stations")
public interface StationService {
    int getStationTimeout = 5000;
    int getStationMaxRetries = 3;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Station> getAllStations();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(getStationTimeout)
    @Retry(maxRetries = getStationMaxRetries, maxDuration = getStationTimeout, delay = 1000)
    Station getStationById(@PathParam("id") String id);
}
