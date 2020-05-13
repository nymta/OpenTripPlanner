package org.opentripplanner.api.resource;

import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.ext.transitlink_window.search.TimeWindowSearch;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.opentripplanner.api.resource.ServerInfo.Q;
import static org.opentripplanner.util.Properties.LOG;

/**
 * A Translink developed class which exposes OTP routing intended to help determine crowding on trains and platforms.
 */
@Path("routers/{routerId}/window_planner")
@XmlRootElement
public class WindowsPlanner extends RoutingResource {

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    @Context
    OTPServer otpServer;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public Response profileRoute (@Context UriInfo uriInfo,
                                  @PathParam("routerId")                        String routerId,
                                  @QueryParam("fromPlace")                      LatLon from,
                                  @QueryParam("toPlace")                        LatLon to,
                                  @QueryParam("fromDateTime")                   String fromDateTimeString,
                                  @QueryParam("toDateTime")                     String toDateTimeString,
                                  @QueryParam("mode")                           QualifiedModeSet modeList,

                                  @QueryParam("routeWhitelist")                 @DefaultValue("") String routeWhiteList,
                                  @QueryParam("excludeStreetEdges")             @DefaultValue("false") boolean excludeStreetEdges,
                                  @QueryParam("showIntermediateStops")          @DefaultValue("false") boolean showIntermediateStops,
                                  @QueryParam("firstTraversalIntoFareEntry")    @DefaultValue("false") boolean firstTraversalIntoFareEntry,
                                  @QueryParam("fareExitOnlyToDestination")      @DefaultValue("false") boolean fareExitOnlyToDestination,
                                  @QueryParam("lastTraversalFromFareExit")      @DefaultValue("false") boolean lastTraversalFromFareExit,
                                  @QueryParam("walkSpeed")                      @DefaultValue("1.4") float walkSpeed, // m/sec

                                  @QueryParam("locale")                         Locale locale
                                  ) throws ParseException {
        /*
         * TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)
         * TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords
         *       or vertex ids to planner (or error back to user)
         * TODO: org.opentripplanner.routing.module.PathServiceImpl has COOORD parsing. Abstract that
         *       out so it's used here too...
         */

        RoutingRequest options = new RoutingRequest(modeList);

        options.setFromString(from.toString());
        options.setToString(to.toString());
        options.walkSpeed = walkSpeed;
        options.showIntermediateStops = showIntermediateStops;

        options.excludeStreetEdges = excludeStreetEdges;
        options.firstTraversalIntoFareEntry = firstTraversalIntoFareEntry;
        options.fareExitOnlyToDestination = fareExitOnlyToDestination;
        options.lastTraversalFromFareExit = lastTraversalFromFareExit;

        try {
            Date fromDateTime = dateFormat.parse(fromDateTimeString);
//            options.setDateTime(fromDateTime);
        } catch (ParseException e) {
            throw e;
        }
        try {
            Date toDateTime = dateFormat.parse(toDateTimeString);
            options.setArriveBy(true);
            options.setDateTime(toDateTime);
        } catch (ParseException e) {
            throw e;
        }
//
//
//        // Create response object, containing a copy of all request parameters. Maybe they should be in the debug section of the response.
        Response response = new Response(uriInfo);
        State requestState = new State(options);

        Router router = null;
        List<GraphPath> paths = null;
        try {


            TimeWindowSearch timeWindowSearch = new TimeWindowSearch(options);

            /* Find some good GraphPaths through the OTP Graph. */
            ShortestPathTree shortestPathTree = timeWindowSearch.findShortestPathTree(requestState);


            TripPlan plan = GraphPathToTripPlanConverter.generatePlan(shortestPathTree.getPaths(), options);

            response.setPlan(plan);

        } catch (Exception e) {
            PlannerError error = new PlannerError(e, locale);
            if(!PlannerError.isPlanningError(e.getClass()))
                LOG.warn("Error while planning path: ", e);
            response.setError(error);
        } finally {
            if (options != null) {
                if (options.rctx != null) {
                    response.debugOutput = options.rctx.debugOutput;
                }
                options.cleanup(); // TODO verify that this cleanup step is being done on Analyst web services
            }
        }

        /* Populate up the elevation metadata */
        response.elevationMetadata = new ElevationMetadata();
        response.elevationMetadata.ellipsoidToGeoidDifference = router.graph.ellipsoidToGeoidDifference;
        response.elevationMetadata.geoidElevation = options.geoidElevation;

        /* Log this request if such logging is enabled. */
        if (options != null && router != null && router.requestLogger != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(' ');
            sb.append(options.arriveBy ? "ARRIVE" : "DEPART");
            sb.append(' ');
            sb.append(LocalDateTime.ofInstant(Instant.ofEpochSecond(options.dateTime), ZoneId.systemDefault()));
            sb.append(' ');
            sb.append(options.modes.getAsStr());
            sb.append(' ');
            sb.append(options.from.lat);
            sb.append(' ');
            sb.append(options.from.lng);
            sb.append(' ');
            sb.append(options.to.lat);
            sb.append(' ');
            sb.append(options.to.lng);
            sb.append(' ');
            if (paths != null) {
                for (GraphPath path : paths) {
                    sb.append(path.getDuration());
                    sb.append(' ');
                    sb.append(path.getTrips().size());
                    sb.append(' ');
                }
            }
            router.requestLogger.info(sb.toString());
        }
        return response;
    }
}
