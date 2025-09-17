package com.example;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/stops")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TrainStopResource {

    @POST
    @Transactional
    public Response create(TrainStop trainStop) {
        trainStop.persist();
        return Response.ok(trainStop).status(201).build();
    }

    @GET
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
    public TrainStop update(@PathParam("id") Long id, TrainStop newTrainStop) {
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