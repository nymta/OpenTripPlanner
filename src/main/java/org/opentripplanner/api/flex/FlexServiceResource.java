package org.opentripplanner.api.flex;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.TripShort;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.model.EncodedPolylineBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Path("routers/{routerId}/flexService/{tripId}")
public class FlexServiceResource {

    private static final Logger LOG = LoggerFactory.getLogger(FlexServiceResource.class);

    private Graph graph;

    private Locale locale;

    private DateTimeFormatter timeFormatter;

    public FlexServiceResource (@Context OTPServer otpServer,
                                @PathParam("routerId") String routerId,
                                @QueryParam("locale") String locale) {
        Router router = otpServer.getRouter(routerId);
        graph = router.graph;
        this.locale = ResourceBundleSingleton.INSTANCE.getLocale(locale);
        timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(this.locale)
                .withZone(graph.getTimeZone().toZoneId());
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public FlexService getFlexService(@PathParam("tripId") String tripIdString, @QueryParam("startIndex") int startIndex) {
        // TODO: what if one trip has multiple areas?
        FeedScopedId tripId = FeedScopedId.convertFromString(tripIdString, ':');
        FlexService flexService = new FlexService();
        Trip trip = graph.index.tripForId.get(tripId);
        flexService.setTrip(new TripShort(trip));
        flexService.setRoute(new RouteShort(trip.getRoute()));
        flexService.setAgency(trip.getRoute().getAgency());
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        Timetable timetable = pattern.scheduledTimetable;
        TripTimes tripTimes = timetable.getTripTimes(trip);

        int startTime = tripTimes.getDepartureTime(startIndex);
        int endTime = tripTimes.getArrivalTime(startIndex + 1);

        String areaId = tripTimes.getServiceArea(startIndex);
        if (areaId != null) {
            Geometry geometry = graph.index.flexAreasById.get(new FeedScopedId(tripId.getAgencyId(), areaId));
            flexService.setServiceArea(getPolyline(geometry));

            for (int i = startIndex; i >= 0 && areaId.equals(tripTimes.getServiceArea(i)); i--) {
                startTime = tripTimes.getDepartureTime(i);
            }
            for (int i = startIndex + 1; i < tripTimes.getNumStops() && areaId.equals(tripTimes.getServiceArea(i)); i++) {
                endTime = tripTimes.getArrivalTime(i);
            }
        }

        double radius = tripTimes.getServiceAreaRadius(startIndex);
        if (radius > 0) {
            flexService.setServiceAreaRadius(radius);

            for (int i = startIndex; i >= 0 && radius == tripTimes.getServiceAreaRadius(i); i--) {
                startTime = tripTimes.getDepartureTime(i);
            }
            for (int i = startIndex + 1; i < tripTimes.getNumStops() && radius == tripTimes.getServiceAreaRadius(i); i++) {
                endTime = tripTimes.getArrivalTime(i);
            }

            Geometry geometry = pattern.geometry;
            flexService.setServiceArea(getPolyline(geometry));
        }

        flexService.setDeviatedService(areaId != null || radius > 0);

        if (!flexService.isDeviatedService()) {
            startTime = tripTimes.getDepartureTime(0);
            endTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);
        }

        flexService.setStartTime(secondsToTimeString(startTime));
        flexService.setEndTime(secondsToTimeString(endTime));

        String pickupMessage = trip.getDrtPickupMessage();
        flexService.setServiceInfo(pickupMessage);

        List<String> days = getDaysOfWeek(trip.getServiceId());
        String dayString = String.join(", ", days);
        flexService.setDays(dayString);

        return flexService;
    }

    private String secondsToTimeString(int seconds) {
        LocalTime localTime = LocalTime.ofSecondOfDay(seconds);
        return timeFormatter.format(localTime);
    }

    private DayOfWeek toDayOfWeek(ServiceDate sd) {
        Calendar calendar = sd.getAsCalendar(graph.getTimeZone());
        LocalDate localDate = LocalDateTime.ofInstant(calendar.toInstant(), graph.getTimeZone().toZoneId()).toLocalDate();
        return localDate.getDayOfWeek();
    }

    private List<String> getDaysOfWeek(FeedScopedId serviceId) {
        CalendarService calendarService = graph.getCalendarService();
        return calendarService.getServiceDatesForServiceId(serviceId).stream()
                .map(this::toDayOfWeek)
                .sorted()
                .distinct()
                .map(d -> d.getDisplayName(TextStyle.FULL, locale))
                .collect(Collectors.toList());
    }

    private EncodedPolylineBean getPolyline(Geometry geometry) {
        if (geometry instanceof Polygon) {
            geometry = ((Polygon) geometry).getExteriorRing();
        }
        return PolylineEncoder.createEncodings(geometry);
    }
}
