package com.example;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
public class TrainStop extends PanacheEntity {
    public String stationId;
    public Instant arrivalTime;
}
