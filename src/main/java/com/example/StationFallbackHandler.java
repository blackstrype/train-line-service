package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

@ApplicationScoped
public class StationFallbackHandler implements FallbackHandler<Station> {

    @ConfigProperty(name = "feature.toggle.station-details-async", defaultValue = "false")
    boolean stationDetailsAsync;

    @Override
    public Station handle(ExecutionContext context) {
        // If the async feature is enabled, propagate exceptions
        if (stationDetailsAsync) {
            Throwable failure = context.getFailure();
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            } else {
                throw new RuntimeException(failure);
            }
        }
        // Otherwise, apply the original fallback logic.
        return StationService.getStationByIdFallback(null); // We can reuse the static method
    }
}
