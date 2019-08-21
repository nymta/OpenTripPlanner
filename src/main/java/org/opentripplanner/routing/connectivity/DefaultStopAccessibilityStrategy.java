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
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;
import java.util.Set;

/**
 * Implement strategy that a stop is accessible only if it has a wheel chair entrance.
 */
public class DefaultStopAccessibilityStrategy extends ConnectivityTemplate<AccessibilityResult> implements StopAccessibilityStrategy {
    @Override
    public AccessibilityResult stopIsAccessible(State state, TransitStop stop) {
        return stop.hasWheelchairEntrance() ? AccessibilityResult.ALWAYS_ACCESSIBLE : AccessibilityResult.NEVER_ACCESSIBLE;
    }

    public DefaultStopAccessibilityStrategy(Graph graph) {
        super(graph);
    }

    @Override
    protected boolean testForEarlyReturn(Vertex v) {
        return false;
    }

    @Override
    protected boolean canUsePathway(State state, PathwayEdge pathway, List<Alert> alerts) {
        return false;
    }

    @Override
    protected AccessibilityResult buildResult(TransitStop tstop, Set<Vertex> vertices, Set<Vertex> accessibles,
                                              List<Alert> alerts, State state, Set<PathwayEdge> links, boolean earlyReturn) {
        return null;
    }
}
