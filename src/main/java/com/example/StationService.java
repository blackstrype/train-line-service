package com.example;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@RegisterRestClient(configKey = "station-service")
@OidcClientFilter("station-service")
@Path("/stations")
@RegisterClientHeaders
public interface StationService {
    final int getStationTimeout = 5000;
    final int getStationMaxRetries = 3;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Station> getAllStations();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(getStationTimeout)
    @Retry(maxRetries = getStationMaxRetries, maxDuration = 5000, delay = 1000)
    Station getStationById(@PathParam("id") String id);
}
