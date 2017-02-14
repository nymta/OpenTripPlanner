package org.opentripplanner.routing.edgetype.temporary;

import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.TripPattern;

/**
 * Created by dbenoff on 2/13/17.
 */
public class TemporaryTripPattern extends TripPattern implements TemporaryEdge {

    public TripPattern originalTripPattern;

    public TemporaryTripPattern(TripPattern tripPattern) {
        super(tripPattern.route, tripPattern.stopPattern);
        this.originalTripPattern = tripPattern;
    }

    @Override
    public void dispose() {
    }
}
