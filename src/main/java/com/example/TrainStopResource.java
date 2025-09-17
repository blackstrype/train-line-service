package com.example;

import io.quarkus.logging.Log;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Path("/stops")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(value = @Tag(name = "Train Stops", description = "Operations for managing train stops."))
public class TrainStopResource {

    @Inject
    @RestClient
    StationService stationService;

    @Operation(summary = "Create a new train stop or retrieve an existing one")
    @APIResponse(responseCode = "201", description = "Train stop created successfully")
    @APIResponse(responseCode = "200", description = "Train stop already exists")
    @APIResponse(responseCode = "400", description = "Invalid request payload")
    @POST
    @Transactional
    public Response create(@Valid TrainStop trainStop) {
        TrainStop existingStop = TrainStop.find("stationId = ?1 and arrivalTime = ?2", trainStop.stationId, trainStop.arrivalTime).firstResult();
        if (existingStop != null) {
            return Response.ok(existingStop).status(200).build();
        }

        // Enrich train stop details
        Station station = stationService.getStationById(trainStop.stationId);
        Log.infof("Found station: %s", station.name);

        trainStop.persist();
        return Response.ok(trainStop).status(201).build();
    }

    @GET
    @RolesAllowed("admin")
    public List<TrainStop> list() {
        return TrainStop.listAll();
    }

    @GET
    @Path("/{id}")
    public TrainStop getById(@PathParam("id") Long id) {
        TrainStop trainStop = TrainStop.findById(id);
        if (trainStop == null) {
            throw new NotFoundException();
        }
        return trainStop;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public TrainStop update(@PathParam("id") Long id, @Valid TrainStop newTrainStop) {
        TrainStop trainStop = TrainStop.findById(id);
        if (trainStop == null) {
            throw new NotFoundException();
        }
        trainStop.stationId = newTrainStop.stationId;
        trainStop.arrivalTime = newTrainStop.arrivalTime;
        trainStop.persist();
        return trainStop;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        TrainStop trainStop = TrainStop.findById(id);
        if (trainStop != null) {
            trainStop.delete();
        }
    }
}