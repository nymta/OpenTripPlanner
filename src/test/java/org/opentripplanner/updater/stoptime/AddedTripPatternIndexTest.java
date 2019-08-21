package org.opentripplanner.updater.stoptime;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class AddedTripPatternIndexTest {

    private static TripPattern addedTripPattern1, addedTripPattern2;

    private static Stop stopA, stopB;

    static {
        Route route = new Route();
        route.setId(new AgencyAndId("feed1", "route1"));
        stopA = stop("stopA");
        stopB = stop("stopB");
        StopPattern pattern1 = new StopPattern(Arrays.asList(
                stopTime(stopA, 1, 1), stopTime(stopB, 2, 2)));
        addedTripPattern1 = new TripPattern(route, pattern1);
        StopPattern pattern2 = new StopPattern(Arrays.asList(
                stopTime(stopB, 3, 3), stopTime(stopA, 4, 4)));
        addedTripPattern2 = new TripPattern(route, pattern2);
    }

    @Test
    public void testNoAddedTrips() {
        AddedTripPatternIndex index = new AddedTripPatternIndex();
        assertEquals(index.getTripPatternsForStop(stopA), Collections.EMPTY_SET);
    }

    @Test
    public void testOneAddedTrip() {
        AddedTripPatternIndex index = new AddedTripPatternIndex();
        index.addTripPattern("feed1", "trip1", new ServiceDate(), addedTripPattern1);
        assertCollectionEquals(index.getTripPatternsForStop(stopB), addedTripPattern1);
    }

    @Test
    public void testMultipleAddedTripDifferentPattern() {
        AddedTripPatternIndex index = new AddedTripPatternIndex();
        index.addTripPattern("feed1", "trip1", new ServiceDate(), addedTripPattern1);
        index.addTripPattern("feed1", "trip2", new ServiceDate(), addedTripPattern2);
        assertCollectionEquals(index.getTripPatternsForStop(stopB), addedTripPattern1, addedTripPattern2);
    }

    @Test
    public void testMultipleAddedTripSamePattern() {
        AddedTripPatternIndex index = new AddedTripPatternIndex();
        index.addTripPattern("feed1", "trip1", new ServiceDate(), addedTripPattern1);
        index.addTripPattern("feed1", "trip2", new ServiceDate(), addedTripPattern1);
        assertCollectionEquals(index.getTripPatternsForStop(stopB), addedTripPattern1);
    }


    private void assertCollectionEquals(Collection<?> collection, Object... array) {
        assertEquals(collection.size(), array.length);
        for (Object obj : array) {
            assertTrue(collection.contains(obj));
        }
    }

    private static StopTime stopTime(Stop stop, int arrival, int departure) {
        StopTime stopTime = new StopTime();
        stopTime.setStop(stop);
        stopTime.setArrivalTime(arrival);
        stopTime.setDepartureTime(departure);
        return stopTime;
    }

    private static Stop stop(String stopId) {
        Stop stop = new Stop();
        stop.setId(new AgencyAndId("feed1", stopId));
        return stop;
    }
}
