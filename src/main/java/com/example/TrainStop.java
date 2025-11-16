package com.example;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Entity
@Schema(name = "TrainStop", description = "Represents a scheduled stop for a train line.")
public class TrainStop extends PanacheEntity {
    @Schema(description = "The ID of the station where the train stops")
    @NotBlank(message = "Station ID must not be blank")
    public String stationId;
    @Schema(description = "The scheduled arrival time at the station in ISO 8601 format")
    @NotNull(message = "Arrival time must not be null")
    public Instant arrivalTime;
    @Schema(description = "The name of the station")
    public String stationName;
}
