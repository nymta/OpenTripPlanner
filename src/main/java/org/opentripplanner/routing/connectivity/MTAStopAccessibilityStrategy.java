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
package org.opentripplanner.routing.connectivity;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Extends default strategy to handle MTASBWY stops by finding first path to accessible entrance:
 * * early return as soon an a wheel chair entrance is found
 * * build AccessibilityResult
 * * only use pathways that are accessible, include alerts in result
 */
public class MTAStopAccessibilityStrategy extends DefaultStopAccessibilityStrategy {

    public MTAStopAccessibilityStrategy(Graph graph) {
        super(graph);
    }

    @Override
    public AccessibilityResult stopIsAccessible(State state, TransitStop stop) {
        if (stop.getStopId().getAgencyId().equals("MTASBWY"))
            return stopIsAccessibleFromStreet(state, stop);
        return super.stopIsAccessible(state, stop);
    }

    private AccessibilityResult stopIsAccessibleFromStreet(State state, TransitStop tstop) {
        // only by traversing pathways, can we get to the street?
        return computeConnectivityResult(state, tstop);
    }

    @Override
    protected boolean testForEarlyReturn(Vertex v) {
        return (v instanceof TransitStop && ((TransitStop) v).isEntrance() && ((TransitStop) v).hasWheelchairEntrance());
    }

    @Override
    protected AccessibilityResult buildResult(TransitStop tstop, Set<Vertex> vertices, Set<Vertex> accessibles,
                                              List<Alert> alerts, State state, Set<PathwayEdge> links, boolean earlyReturn) {
        if (earlyReturn) {
            return AccessibilityResult.ALWAYS_ACCESSIBLE;
        } else {
            return AccessibilityResult.notAccessibleForReason(alerts);
        }
    }

    @Override
    protected boolean canUsePathway(State state, PathwayEdge pathway, List<Alert> alerts) {
        if (!pathway.isWheelchairAccessible())
            return false;
        if (!pathway.isElevator())
            return true;
        if (state.getOptions().ignoreRealtimeUpdates)
            return true;
        List<Alert> newAlerts = pathway.getElevatorIsOutOfServiceAlerts(graph, state);
        alerts.addAll(newAlerts);
        return newAlerts.isEmpty();
    }
}
