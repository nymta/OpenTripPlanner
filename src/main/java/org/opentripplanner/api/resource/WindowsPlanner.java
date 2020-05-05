package org.opentripplanner.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A Translink developed class which exposes OTP routing intended to help determine crowding on trains and platforms.
 */
@Path("routers/{routerId}/window_planner")
@XmlRootElement
public class WindowsPlanner {

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public javax.ws.rs.core.Response profileRoute (){
//            @QueryParam("from") LatLon from,
//            @QueryParam("fromStop") String fromStopString;
        int i = 1+1;


        return Response.ok().entity("OK\n").build();
    }
}
