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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Station> getAllStations();

    @GET
    @Path("/{id}")
    @Timeout(1000)
    @Retry(maxRetries = 3, maxDuration = 5000)
    @Produces(MediaType.APPLICATION_JSON)
    Station getStationById(@PathParam("id") String id);
}
