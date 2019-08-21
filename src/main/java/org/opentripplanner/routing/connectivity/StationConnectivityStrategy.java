/*
 *  This program is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation, either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentripplanner.routing.connectivity;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;
import java.util.Set;

/**
 * Implements a complete traversal of the pathway edges connected to a given stop:
 * * no early return
 * * build StationConnectivityResult
 * * can use any pathway except an elevator with an alert, include alerts in result
 */
public class StationConnectivityStrategy extends ConnectivityTemplate<StationConnectivityResult> {
    public StationConnectivityStrategy(Graph graph) {
        super(graph);
    }

    @Override
    protected boolean testForEarlyReturn(Vertex v) {
        return false;
    }

    @Override
    protected StationConnectivityResult buildResult(TransitStop tstop,
                                                    Set<Vertex> vertices,
                                                    Set<Vertex> accessibles,
                                                    List<Alert> alerts,
                                                    State state,
                                                    Set<PathwayEdge> links,
                                                    boolean earlyReturn) {
        return new StationConnectivityResult(tstop, vertices, accessibles, alerts, graph, state, links, earlyReturn);
    }

    @Override
    protected boolean canUsePathway(State state, PathwayEdge pathway, List<Alert> alerts) {
        if (!pathway.isElevator())
            return true;
        if (state.getOptions().ignoreRealtimeUpdates)
            return true;
        List<Alert> newAlerts = pathway.getElevatorIsOutOfServiceAlerts(graph, state);
        alerts.addAll(newAlerts);
        return newAlerts.isEmpty();
    }

}
