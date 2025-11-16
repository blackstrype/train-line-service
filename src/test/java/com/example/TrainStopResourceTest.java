package com.example;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class TrainStopResourceTest {

    @InjectMock
    @RestClient
    StationService stationService;

    @ConfigProperty(name = "feature.toggle.station-details-async")
    boolean stationDetailsAsync;
    private int expectedCreateStatusCode;

    @BeforeEach
    @Transactional
    public void setup()
    {
        // Set the expected status code based on the feature toggle
        expectedCreateStatusCode = stationDetailsAsync ? 202 : 201;

        TrainStop.deleteAll();

        Station mockStation = new Station();
        mockStation.name = "Mocked Central Station";
        mockStation.location = "Downtown";

        Mockito.when(stationService.getStationById(Mockito.anyString())).thenReturn(mockStation);
        Mockito.when(stationService.getAllStations()).thenReturn(List.of(mockStation));
    }

    @Test
    public void testCreateTrainStop() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                {
                    "stationId": "station-1",
                    "arrivalTime": "2025-09-16T10:00:00Z"
                }
            """)
                .when().post("/stops")
                .then()
                .statusCode(expectedCreateStatusCode)
                .body("stationId", is("station-1"));
    }

    @Test
    public void testListAllTrainStops() {
        // We will create some stops first to ensure the list is not empty
        given().contentType("application/json").body("""
        {
          "stationId": "station-2",
          "arrivalTime": "2025-09-16T10:00:00Z"
        }
        """).when().post("/stops").then()
                .statusCode(expectedCreateStatusCode)
                .body("stationId", is("station-2"));
        given().contentType("application/json").body("""
        {
          "stationId": "station-3",
          "arrivalTime": "2025-09-16T10:05:00Z"
        }
        """).when().post("/stops").then()
                .statusCode(expectedCreateStatusCode)
                .body("stationId", is("station-3"));

        given()
                .when().get("/stops")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    public void testGetTrainStopById() {
        // First create a stop to update
        String createdStop = given().contentType("application/json").body("""
        {
          "stationId": "station-4",
          "arrivalTime": "2025-09-16T11:00:00Z"
        }
        """).when().post("/stops").then()
                .statusCode(expectedCreateStatusCode)
                .extract().asString();
        long stopId = Long.parseLong(createdStop.split(":")[1].split(",")[0]);

        given()
                .when().get("/stops/" + stopId)
                .then()
                .statusCode(200)
                .body("stationId", is("station-4"));
    }

    @Test
    public void testUpdateTrainStop() {
        // First create a stop to update
        String createdStop = given().contentType("application/json").body("""
        {
          "stationId": "station-5",
          "arrivalTime": "2025-09-16T11:00:00Z"
        }
        """).when().post("/stops").then()
                .statusCode(expectedCreateStatusCode)
                .extract().asString();
        long stopId = Long.parseLong(createdStop.split(":")[1].split(",")[0]);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                {
                    "stationId": "station-6",
                    "arrivalTime": "2025-09-16T11:00:00Z"
                }
            """)
                .when().put("/stops/" + stopId)
                .then()
                .statusCode(200)
                .body("stationId", is("station-6"));
    }

    @Test
    public void testDeleteTrainStop() {
        // First create a stop to delete
        String createdStop = given().contentType("application/json").body("""
        {
          "stationId": "station-7",
          "arrivalTime": "2025-09-16T12:00:00Z"
        }
        """).when().post("/stops").then()
                .statusCode(expectedCreateStatusCode)
                .extract().asString();
        long stopId = Long.parseLong(createdStop.split(":")[1].split(",")[0]);

        given()
                .when().delete("/stops/" + stopId)
                .then()
                .statusCode(204);
    }
}