package com.example;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@RegisterRestClient(configKey = "station-service")
@OidcClientFilter("station-service")
@Path("/stations")
@RegisterClientHeaders
public interface StationService {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Station> getAllStations();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Station getStationById(@PathParam("id") String id);
}
