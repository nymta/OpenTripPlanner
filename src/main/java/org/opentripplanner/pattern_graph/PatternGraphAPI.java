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
import org.opentripplanner.pattern_graph.model.StopNodeAttribute;
import org.opentripplanner.profile.StopCluster;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    @QueryParam("date")
    private String date;

    @QueryParam("time")
    private String time;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PatternGraph getGraph() {

        String dateTime = "2018-12-25" + ' ' + time;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mma");
        Date timeOfInterest;
        //Date midnight;
        try {
            timeOfInterest = format.parse(dateTime);
            //midnight = format.parse(midnightString);
        } catch (Exception e){
            timeOfInterest = null;
            //midnight = null;
        }
        //long secs_since_midnight = (timeOfInterest.getTime() - midnight.getTime())/1000;
        AgencyAndId routeId = AgencyAndId.convertFromString(this.routeId, ':');

        Route route = index.routeForId.get(routeId);
        StopNodeAttribute attribute = new StopNodeAttribute();
        attribute.setColor("#"+route.getColor());

        Collection<TripPattern> patterns = index.patternsForRoute.get(route);

        Map<String, StopNode> nodeForId = new HashMap<>();

        for (TripPattern pattern : patterns) {
            if (!Integer.toString(pattern.directionId).equals(directionId) || !pattern.operatingAt(timeOfInterest)) {
                continue;
            }
            StopNode prev = null;
            for (Stop stop : pattern.getStops()) {

                StopCluster cluster = index.stopClusterForStop.get(stop);
                StopNode node = nodeForId.computeIfAbsent(cluster.id, StopNode::new);

                node.setAttributes(new StopShort(stop));
                node.setNodeAttribute(attribute);
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
