package com.example;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/status")
public class StatusResource {

    @ConfigProperty(name = "train-line-name")
    String trainLineName;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String status() {
        Log.info("Status check successful for " + trainLineName);
        return "Operational";
    }
}