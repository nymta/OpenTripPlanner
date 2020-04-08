package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.ApiRouterInfo;
import org.opentripplanner.api.model.ApiRouterList;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import static org.opentripplanner.api.resource.ServerInfo.Q;

/**
 * This REST API endpoint returns some meta-info about a router. OTP2 does no longer support
 * remotely loading, reloading, and evicting graphs on a running server (Supported in OTP1).
 * <p>
 * The HTTP verbs are used as follows:
 * <p>
 * GET - see the registered routerIds(there is just one: default) with the graph.
 * <p>
 * The HTTP request URLs are of the form /otp/routers/{routerId}. The {routerId} is kept to be
 * backward compatible, but the value is ignored. There is only one router, the "default" and
 * that is returned - even if you specify something else.
 */
@Path("/routers")
@PermitAll // exceptions on methods
public class Routers {

    @Context
    protected OTPServer otpServer;

    /**
     * Return the "default" router information.
     */
    @GET
    @Path("{routerId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q})
    public ApiRouterInfo getGraphId(@PathParam("routerId") String routerId) {
        return getRouterInfo();
    }

    /**
     * Returns a list of routers and their bounds. A list with one item, the "default" router,
     * is returned.
     * @return a representation of the graphs and their geographic bounds, in JSON or XML depending
     * on the Accept header in the HTTP request.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q})
    public ApiRouterList getRouterIds() {
        ApiRouterList routerList = new ApiRouterList();
        routerList.routerInfo.add(getRouterInfo());
        return routerList;
    }

    private ApiRouterInfo getRouterInfo() {
        try {
            Router router = otpServer.getRouter(null);
            return new ApiRouterInfo("default", router.graph);
        }
        catch (GraphNotFoundException e) {
            return null;
        }
    }
}
