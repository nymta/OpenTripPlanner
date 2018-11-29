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
package org.opentripplanner.pattern_graph;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.index.model.StopTimesByStop;
import org.opentripplanner.pattern_graph.model.PatternGraph;
import org.opentripplanner.pattern_graph.model.StopNode;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Path("/routers/{routerId}/patternGraph")
@Produces(MediaType.APPLICATION_JSON)
public class PatternGraphAPI {

    private Router router;

    private GraphIndex index;

    public PatternGraphAPI(@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        this.router = router;
        index = router.graph.index;
    }

    @QueryParam("routeId")
    private String routeId;

    @QueryParam("directionId")
    private String directionId;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PatternGraph getGraph() {
        AgencyAndId routeId = AgencyAndId.convertFromString(this.routeId, ':');

        Route route = index.routeForId.get(routeId);
        Collection<TripPattern> patterns = index.patternsForRoute.get(route);

        Map<AgencyAndId, StopNode> nodeForId = new HashMap<>();

        for (TripPattern pattern : patterns) {
            if (!Integer.toString(pattern.directionId).equals(directionId)) {
                continue;
            }
            StopNode prev = null;
            for (Stop stop : pattern.getStops()) {
                StopNode node = nodeForId.computeIfAbsent(stop.getId(), StopNode::new);
                node.setStop(new StopShort(stop));
                if (prev != null) {
                    prev.addSuccessor(node);
                }
                prev = node;
            }
        }

        PatternGraph graph = new PatternGraph();
        graph.setNodes(new ArrayList<>(nodeForId.values()));
        return graph;
    }
}
