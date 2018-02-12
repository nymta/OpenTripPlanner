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
package org.opentripplanner.api.resource;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;

import org.opentripplanner.index.model.PatternDetail;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

import static org.opentripplanner.api.resource.ServerInfo.Q;

/**
 *  Return patterns for route
 *
 *  Conceptually this is similar to IndexAPI.getPatternsForRoute, but includes more details and filtering options
 */
@Path("/routers/{routerId}/patternsForRoute")
@XmlRootElement
public class RoutePatternsResource {

    /**
     * Route to return patterns for
     */
    @QueryParam("route")
    private String routeStr;

    /**
     * Epoch time in miliseconds during which the user is expecting the pattern
     * If returnAllPatterns=true, this parameter is ignored.
     */
    @QueryParam("time")
    private Long time;


    /**
     * If true, return all patterns for route, otherwise, return only patterns
     * that are currently in service;
     */
    @QueryParam("returnAllPatterns")
    private boolean returnAllPatterns = false;

    private GraphIndex index;

    public RoutePatternsResource(@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        index = router.graph.index;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q})
    public List<PatternDetail> getPatternsInService() {
        List<PatternDetail> patterns = new ArrayList<PatternDetail>();

        AgencyAndId routeId = AgencyAndId.convertFromString(routeStr, ':');
        Route route = index.routeForId.get(routeId);

        Date date;
        ServiceDate serviceDate;
        ArrayList<ServiceDay> serviceDays = new ArrayList<ServiceDay>();

        if (returnAllPatterns)
        {
            if (route != null) {
                Collection<TripPattern> tripPatterns = index.patternsForRoute.get(route);
                for (TripPattern pattern : tripPatterns) {
                    patterns.add(new PatternDetail(pattern));
                }
            }
        }
        else {
            if (!time.equals(null)) {
                date = new Date();
                serviceDate = new ServiceDate(date);
            } else {
                date = new Date(time);
                serviceDate = new ServiceDate(date);
            }

            serviceDays = getServiceDays(index.graph, date);
            if (route != null) {
                Collection<TripPattern> tripPatterns = index.patternsForRoute.get(route);

                for (TripPattern pattern : tripPatterns) {
                    for (ServiceDay serviceDay : serviceDays)
                    {
                        Timetable timetable = pattern.getUpdatedTimetable(options, serviceDay);

                    }


                    List<Trip> trips = pattern.getTrips();

                    for (Trip trip : trips) {
                        List<TripTimes> tripTimesList = pattern.scheduledTimetable.tripTimes;

                        for(TripTimes tripTimes : tripTimesList){
                            tripTimes.
                        }

                    }
//                pattern.scheduledTimetable;

                    patterns.add(new PatternDetail(pattern));
                }

                return patterns;
            }
        }



        return null;
    }

    /**
     * Cache ServiceDay objects representing which services are running yesterday, today, and tomorrow relative to the search time. This information
     * is very heavily used (at every transit boarding) and Date operations were identified as a performance bottleneck. Must be called after the
     * TraverseOptions already has a CalendarService set.
     */
    private ArrayList<ServiceDay> getServiceDays(Graph graph, Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setTimeZone(graph.getTimeZone());
        CalendarService calendarService = graph.getCalendarService();

        final ServiceDate serviceDate = new ServiceDate(c);
        ArrayList<ServiceDay> serviceDays = new ArrayList<ServiceDay>(3);

        for (String feedId : graph.getFeedIds()) {
            for (Agency agency : graph.getAgencies(feedId)) {
                addIfNotExists(serviceDays, new ServiceDay(graph, serviceDate.previous(), calendarService, agency.getId()));
                addIfNotExists(serviceDays, new ServiceDay(graph, serviceDate, calendarService, agency.getId()));
                addIfNotExists(serviceDays, new ServiceDay(graph, serviceDate.next(), calendarService, agency.getId()));
            }
        }

        return serviceDays;
    }

    private static <T> void addIfNotExists(ArrayList<T> list, T item) {
        if (!list.contains(item)) {
            list.add(item);
        }
    }
}
