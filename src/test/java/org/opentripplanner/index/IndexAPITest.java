/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.index;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.





        StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.index.model.PatternDetail;
import org.opentripplanner.index.model.PatternShort;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.StopDetail;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TransferShort;
import org.opentripplanner.index.model.TripShort;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.TestUtils;
import org.opentripplanner.util.model.EncodedPolylineBean;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * This does some basic tests on the IndexAPI - at a bare minimum, just test the types of the
 * Response. More complex tests are added as necessary. (For example, when adding ADDED trip support
 * to the trips calls.) Some methods still need to be tested.
 */
public class IndexAPITest {

    private static String feedId;

    private static IndexAPI api;

    @BeforeClass
    public static void setup() throws Exception {
        Graph graph = new Graph();
        GtfsBundle gtfsBundle = new GtfsBundle(new File(ConstantsForTests.FAKE_GTFS));
        GtfsFeedId gtfsFeedId = new GtfsFeedId.Builder().id("FEED").build();
        gtfsBundle.setFeedId(gtfsFeedId);
        gtfsBundle.setTransfersTxtDefinesStationPaths(true);
        List<GtfsBundle> gtfsBundleList = Collections.singletonList(gtfsBundle);
        GtfsModule module = new GtfsModule(gtfsBundleList);
        module.buildGraph(graph, new HashMap<>());
        graph.index(new DefaultStreetVertexIndexFactory());
        feedId = "FEED";
        api = new IndexAPI(graph);

        long time = TestUtils.dateInSeconds("America/New_York", 2018, 0, 1, 4, 0, 0);
        graph.index.setTime(time * 1000);

        TripUpdate update42 = TripUpdate.newBuilder()
                .setTrip(TripDescriptor.newBuilder().setTripId("4.2")
                        .setStartDate("20180101"))
                .addStopTimeUpdate(StopTimeUpdate.newBuilder()
                        .setStopId("F")
                        .setArrival(StopTimeEvent.newBuilder().setDelay(300))
                        .setDeparture(StopTimeEvent.newBuilder().setDelay(300)))
                .addStopTimeUpdate(StopTimeUpdate.newBuilder()
                        .setStopId("G")
                        .setArrival(StopTimeEvent.newBuilder().setDelay(200))
                        .setDeparture(StopTimeEvent.newBuilder().setDelay(200)))
                .addStopTimeUpdate(StopTimeUpdate.newBuilder()
                        .setStopId("H")
                        .setArrival(StopTimeEvent.newBuilder().setDelay(100))
                        .setDeparture(StopTimeEvent.newBuilder().setDelay(100)))
                .build();
        TripUpdate update172 = TripUpdate.newBuilder()
                .setTrip(TripDescriptor.newBuilder().setTripId("17.2")
                        .setStartDate("20180101")
                        .setRouteId("17")
                        .setScheduleRelationship(TripDescriptor.ScheduleRelationship.ADDED))
                .addStopTimeUpdate(StopTimeUpdate.newBuilder()
                        .setStopId("C")
                        .setArrival(StopTimeEvent.newBuilder().setTime(1514844000)) // 5pm
                        .setDeparture(StopTimeEvent.newBuilder().setTime(1514844000)))
                .addStopTimeUpdate(StopTimeUpdate.newBuilder()
                        .setStopId("E")
                        .setArrival(StopTimeEvent.newBuilder().setTime(1514844600)) // 5:10pm
                        .setDeparture(StopTimeEvent.newBuilder().setTime(1514844600)))
                .build();
        List<TripUpdate> updates = Arrays.asList(update42, update172);
        graph.timetableSnapshotSource = new TimetableSnapshotSource(graph);
        graph.timetableSnapshotSource.purgeExpiredData = false;
        graph.timetableSnapshotSource.applyTripUpdates(graph, true, updates, feedId);
    }

    @Test
    public void testFeeds() {
        List<String> feeds = getResponseList(api.getFeeds(), String.class);
        assertEquals(Collections.singletonList(feedId), feeds);
    }

    @Test
    public void testFeedInfo() {
        // no feed_info in this GTFS
        Response resp = api.getFeedInfo(feedId);
        assertEquals(Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testAgencies() {
        List<Agency> agencies = getResponseList(api.getAgencies(feedId), Agency.class);
        assertEquals(1, agencies.size());
        assertEquals("Fake Agency", agencies.iterator().next().getName());
    }

    @Test
    public void testAgency() {
        Agency agency = getResponse(api.getAgency(feedId, "agency"), Agency.class);
        assertEquals("Fake Agency", agency.getName());
    }

    @Test
    public void testAgencyRoutes() {
        List<RouteShort> routes = getResponseList(api.getAgencyRoutes(feedId, "agency"), RouteShort.class);
        assertEquals(18, routes.size());
    }

    @Test
    public void testStop() {
        Stop stop = getResponse(api.getStop(feedId + ":A"), Stop.class);
        assertEquals("A", stop.getId().getId());
        assertEquals("A", stop.getName());
    }

    @Test
    public void testStopsInBbox() {
        List<StopDetail> stops = getResponseList(api.getStopsInRadius(40d, -75d, 41d, -74d, null, null, null, false), StopDetail.class);
        List<String> stopIds = stops.stream().map(s -> s.id.getId()).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("F", "K", "N"), stopIds);
    }

    @Test
    public void testRoutesForStop() {
        List<RouteShort> routes = getResponseList(api.getRoutesForStop(feedId + ":E"), RouteShort.class);
        List<String> routeIds = routes.stream().map(r -> r.id.getId()).sorted().collect(Collectors.toList());
        // alphabetic sort
        assertEquals(Arrays.asList("17", "18", "3", "9"), routeIds);
    }

    // route 4 goes thru stop F, and has 3 trips which go F,G,H
    @Test
    public void testPatternsForStop() {
        List<PatternShort> patterns = getResponseList(api.getPatternsForStop(feedId + ":F"), PatternShort.class);
        assertEquals(1, patterns.size());
    }

    @Test
    public void testStoptimesForStop() {
        long time = TestUtils.dateInSeconds("America/New_York", 2018, 0, 1, 4, 0, 0);
        List<StopTimesInPattern> stopTimes = getResponseList(api.getStoptimesForStop(feedId + ":G",
                time, 86400, 3, false), StopTimesInPattern.class);
        assertEquals(1, stopTimes.size());
        StopTimesInPattern stopTimesInPattern = stopTimes.get(0);
        assertEquals("4", stopTimesInPattern.route.id.getId());

        List<TripTimeShort> tripTimes = new ArrayList<>(stopTimesInPattern.times);
        tripTimes.sort(Comparator.comparingInt(TripTimeShort::getRealtimeArrival));

        TripTimeShort tripTimes41 = tripTimes.get(0);
        assertEquals(5 * 3600 + (30 * 60), tripTimes41.realtimeArrival);
        assertEquals(5 * 3600 + (30 * 60), tripTimes41.realtimeDeparture);
        assertFalse(tripTimes41.realtime);

        TripTimeShort tripTimes42 = tripTimes.get(1);
        assertEquals(23 * 3600 + (30 * 60) + 200, tripTimes42.realtimeArrival);
        assertEquals(23 * 3600 + (30 * 60), tripTimes42.scheduledArrival);
        assertEquals(23 * 3600 + (30 * 60) + 200, tripTimes42.realtimeDeparture);
        assertEquals(23 * 3600 + (30 * 60), tripTimes42.scheduledDeparture);
        assertTrue(tripTimes42.realtime);

        TripTimeShort tripTimes43 = tripTimes.get(2);
        assertEquals(24 * 3600 + (10 * 60), tripTimes43.realtimeArrival);
        assertEquals(24 * 3600 + (10 * 60), tripTimes43.realtimeDeparture);
        assertFalse(tripTimes43.realtime);
    }

    @Test
    public void testStoptimesForStopAndDate() {
        List<StopTimesInPattern> stopTimes = getResponseList(api.getStoptimesForStopAndDate(feedId + ":G", "20180101", false),
                StopTimesInPattern.class);
        assertEquals(1, stopTimes.size());
        StopTimesInPattern stopTimesInPattern = stopTimes.get(0);
        assertEquals("4", stopTimesInPattern.route.id.getId());

        List<TripTimeShort> tripTimes = new ArrayList<>(stopTimesInPattern.times);
        tripTimes.sort(Comparator.comparingInt(TripTimeShort::getRealtimeArrival));

        TripTimeShort tripTimes41 = tripTimes.get(0);
        assertEquals(5 * 3600 + (30 * 60), tripTimes41.realtimeArrival);
        assertEquals(5 * 3600 + (30 * 60), tripTimes41.realtimeDeparture);
        assertFalse(tripTimes41.realtime);

        TripTimeShort tripTimes42 = tripTimes.get(1);
        assertTrue(tripTimes42.realtime);

        assertEquals(23 * 3600 + (30 * 60) + 200, tripTimes42.realtimeArrival);
        assertEquals(23 * 3600 + (30 * 60), tripTimes42.scheduledArrival);
        assertEquals(23 * 3600 + (30 * 60) + 200, tripTimes42.realtimeDeparture);
        assertEquals(23 * 3600 + (30 * 60), tripTimes42.scheduledDeparture);
    }

    @Test
    public void testTransfers() {
        List<TransferShort> transfers = getResponseList(api.getTransfers(feedId + ":L"), TransferShort.class);
        assertEquals(1, transfers.size());
        TransferShort transfer = transfers.get(0);
        assertEquals(feedId + ":K", transfer.toStopId);
    }

    @Test
    public void testRoutes() {
        List<RouteShort> routes = getResponseList(api.getRoutes(null,
                Collections.singletonList(feedId + ":E")), RouteShort.class);
        List<String> routeIds = routes.stream().map(r -> r.id.getId()).sorted().collect(Collectors.toList());
        // alphabetic sort
        assertEquals(Arrays.asList("17", "18", "3", "9"), routeIds);
    }

    @Test
    public void testRoute() {
        Route route = getResponse(api.getRoute(feedId + ":4"), Route.class);
        assertEquals("4", route.getId().getId());
    }

    // route 4 goes thru stop F, and has 3 trips which go F,G,H
    @Test
    public void testPatternsForRoute() {
        List<PatternShort> patterns = getResponseList(api.getPatternsForRoute(feedId + ":4"), PatternShort.class);
        assertEquals(1, patterns.size());
    }

    @Test
    public void testStopsForRoute() {
        List<StopShort> stops = getResponseList(api.getStopsForRoute(feedId + ":4"), StopShort.class);
        List<String> stopIds = stops.stream().map(s -> s.id.getId()).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("F", "G", "H"), stopIds);
    }

    @Test
    public void testTripsForRoute() {
        List<TripShort> trips = getResponseList(api.getTripsForRoute(feedId + ":4"), TripShort.class);
        List<String> stopIds = trips.stream().map(t -> t.id.getId()).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("4.1", "4.2", "4.3"), stopIds);
    }

    @Test
    public void testTrip() {
        Trip trip = getResponse(api.getTrip(feedId + ":4.1"), Trip.class);
        assertEquals("4.1", trip.getId().getId());
        assertEquals("4", trip.getRoute().getId().getId());
    }

    @Test
    public void testStopsForTrip() {
        List<StopShort> stops = getResponseList(api.getStopsForTrip(feedId + ":4.1"), StopShort.class);
        List<String> ids = stops.stream().map(s -> s.id.getId()).collect(Collectors.toList());
        assertEquals(Arrays.asList("F", "G", "H"), ids);
    }

    @Test
    public void testSemanticHashForTrip() {
        // just confirm types
        getResponse(api.getSemanticHashForTrip(feedId + ":4.1"), String.class);
    }

    @Test
    public void testStopTimesForTrip() {
        List<TripTimeShort> times = getResponseList(api.getStoptimesForTrip(feedId + ":4.1", null), TripTimeShort.class);
        // 5:00 F, 5:30 G, 6:00 H
        assertEquals(3, times.size());
        TripTimeShort f = times.get(0);
        assertEquals("F", f.stopId.getId());
        assertEquals(0, f.stopIndex);
        assertEquals(5 * 3600, f.scheduledArrival);
        TripTimeShort g = times.get(1);
        assertEquals("G", g.stopId.getId());
        assertEquals(1, g.stopIndex);
        assertEquals((5 * 3600) + (30 * 60), g.scheduledArrival);
        TripTimeShort h = times.get(2);
        assertEquals("H", h.stopId.getId());
        assertEquals(2, h.stopIndex);
        assertEquals(6 * 3600, h.scheduledArrival);
    }

    @Test
    public void testGeometryForTrip() {
        getResponse(api.getGeometryForTrip(feedId + ":4.1"), EncodedPolylineBean.class);
    }

    @Test
    public void testPatterns() {
        getResponseList(api.getPatterns(), PatternShort.class);
    }

    @Test
    public void testPatternDetail() {
        List<PatternShort> patterns = getResponseList(api.getPatternsForStop(feedId + ":F"), PatternShort.class);
        PatternShort pattern = patterns.get(0);
        String patternId = pattern.id;
        PatternDetail patternDetail = getResponse(api.getPattern(patternId), PatternDetail.class);
        List<String> stopIds = patternDetail.stops.stream().map(s -> s.id.getId()).collect(Collectors.toList());
        assertEquals(Arrays.asList("F", "G", "H"), stopIds);
        List<String> tripIds = patternDetail.trips.stream().map(t -> t.id.getId()).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("4.1", "4.2", "4.3"), tripIds);
    }

    @Test
    public void testTripsForPattern() {
        List<PatternShort> patterns = getResponseList(api.getPatternsForStop(feedId + ":F"), PatternShort.class);
        PatternShort pattern = patterns.get(0);
        String patternId = pattern.id;
        List<TripShort> trips = getResponseList(api.getTripsForPattern(patternId), TripShort.class);
        List<String> tripIds = trips.stream().map(t -> t.id.getId()).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("4.1", "4.2", "4.3"), tripIds);
    }

    @Test
    public void testStopsForPattern() {
        List<PatternShort> patterns = getResponseList(api.getPatternsForStop(feedId + ":F"), PatternShort.class);
        PatternShort pattern = patterns.get(0);
        String patternId = pattern.id;
        List<StopShort> stops = getResponseList(api.getStopsForPattern(patternId), StopShort.class);
        List<String> stopIds = stops.stream().map(s -> s.id.getId()).collect(Collectors.toList());
        assertEquals(Arrays.asList("F", "G", "H"), stopIds);
    }

    @Test
    public void testSemanticHashForPattern() {
        List<PatternShort> patterns = getResponseList(api.getPatternsForStop(feedId + ":F"), PatternShort.class);
        PatternShort pattern = patterns.get(0);
        String patternId = pattern.id;
        getResponse(api.getSemanticHashForPattern(patternId), String.class);
    }

    @Test
    public void testGeometryForPattern() {
        List<PatternShort> patterns = getResponseList(api.getPatternsForStop(feedId + ":F"), PatternShort.class);
        PatternShort pattern = patterns.get(0);
        String patternId = pattern.id;
        getResponse(api.getGeometryForPattern(patternId), EncodedPolylineBean.class);
    }

    // Realtime: SCHEDULED trip

    @Test
    public void testRealtimeStopTimesForTrip() {
        List<TripTimeShort> times = getResponseList(api.getStoptimesForTrip(feedId + ":4.2", null), TripTimeShort.class);
        // 23:00 F, 23:30 G, 24:00 H, plus 5 min
        assertEquals(3, times.size());
        TripTimeShort f = times.get(0);
        assertEquals("F", f.stopId.getId());
        assertEquals(0, f.stopIndex);
        assertTrue(f.realtime);
        assertEquals(23 * 3600, f.scheduledArrival);
        assertEquals((23 * 3600) + 300, f.realtimeArrival);
        assertEquals(300, f.arrivalDelay);
        TripTimeShort g = times.get(1);
        assertEquals("G", g.stopId.getId());
        assertEquals(1, g.stopIndex);
        assertTrue(g.realtime);
        assertEquals((23 * 3600) + (30 * 60), g.scheduledArrival);
        assertEquals((23 * 3600) + (30 * 60) + 200, g.realtimeArrival);
        assertEquals(200, g.arrivalDelay);
        TripTimeShort h = times.get(2);
        assertEquals("H", h.stopId.getId());
        assertEquals(2, h.stopIndex);
        assertTrue(h.realtime);
        assertEquals(24 * 3600, h.scheduledArrival);
        assertEquals(24 * 3600 + 100, h.realtimeArrival);
        assertEquals(100, h.arrivalDelay);
    }

    // ADDED trips

    @Test
    public void testStopTimesForStopAddedTrip() {
        long time = TestUtils.dateInSeconds("America/New_York", 2018, 0, 1, 16, 0, 0);
        List<StopTimesInPattern> stopTimesInPatterns = getResponseList(api.getStoptimesForStop(feedId + ":C",
                time, 86400, 3, false), StopTimesInPattern.class);

        // filter to route 17 and order by arrival time
        List<TripTimeShort> tripTimes = stopTimesInPatterns.stream()
                .filter(st -> st.route.id.getId().equals("17"))
                .flatMap(st -> st.times.stream())
                .sorted(Comparator.comparingLong(tt -> tt.realtimeArrival + tt.serviceDay))
                .collect(Collectors.toList());

        long today = new ServiceDate(2018, 1, 1).getAsDate().getTime() / 1000;

        TripTimeShort tripTimes171 = tripTimes.get(0);
        assertEquals("17.1", tripTimes171.tripId.getId());
        assertEquals(16 * 3600, tripTimes171.realtimeArrival);
        assertEquals(16 * 3600, tripTimes171.realtimeDeparture);
        assertEquals(today, tripTimes171.serviceDay);
        assertFalse(tripTimes171.realtime);
        assertEquals(RealTimeState.SCHEDULED, tripTimes171.realtimeState);

        TripTimeShort tripTimes172 = tripTimes.get(1);
        assertEquals("17.2", tripTimes172.tripId.getId());
        assertEquals(17 * 3600, tripTimes172.realtimeArrival);
        assertEquals(17 * 3600, tripTimes172.realtimeDeparture);
        assertEquals(today, tripTimes172.serviceDay);
        assertTrue(tripTimes172.realtime);
        assertEquals(RealTimeState.ADDED, tripTimes172.realtimeState);

    }

    @Test
    public void testStopsForAddedTrip() {
        List<StopShort> stops = getResponseList(api.getStopsForTrip(feedId + ":17.2"), StopShort.class);
        List<String> ids = stops.stream().map(s -> s.id.getId()).collect(Collectors.toList());
        assertEquals(Arrays.asList("C", "E"), ids);
    }

    @Test
    public void testSemanticHashForAddedTrip() {
        // just confirm types
        getResponse(api.getSemanticHashForTrip(feedId + ":17.2"), String.class);
    }

    @Test
    public void testStopTimesForAddedTrip() {
        List<TripTimeShort> times = getResponseList(api.getStoptimesForTrip(feedId + ":17.2", null), TripTimeShort.class);
        // 5:00 F, 5:30 G, 6:00 H
        assertEquals(2, times.size());
        TripTimeShort c = times.get(0);
        assertEquals("C", c.stopId.getId());
        assertEquals(0, c.stopIndex);
        assertEquals(17 * 3600, c.scheduledArrival);
        TripTimeShort e = times.get(1);
        assertEquals("E", e.stopId.getId());
        assertEquals(1, e.stopIndex);
        assertEquals((17 * 3600) + (10 * 60), e.scheduledArrival);
    }

    @Test
    public void testTripAdded() {
        Trip trip = getResponse(api.getTrip(feedId + ":17.2"), Trip.class);
        assertEquals("17.2", trip.getId().getId());
    }

    private <T> T getResponse(Response response, Class<T> klass) {
        Object obj = response.getEntity();
        assertTrue(klass.isInstance(obj));
        return klass.cast(obj);
    }

    private <T> List<T> getResponseList(Response response, Class<T> klass) {
        Object obj = response.getEntity();
        assertTrue(Collection.class.isInstance(obj));
        List<T> values = new ArrayList<T>();
        for (Object o : (Collection) obj) {
            assertTrue(klass.isInstance(o));
            values.add(klass.cast(o));
        }
        return values;
    }

}
