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

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.alertpatch.LocalizedAlert;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.*;

/**
 * Bundles connectivity elements, nodes, links, and alerts, into one model.
 */
public class StationConnectivityResult {
    private String stationName;
    private String stationId;
    private Set<StationNode> nodes;
    private List<LocalizedAlert> alerts;
    private Set<StationLink> links;

    public String getStationId() {
        return stationId;
    }

    public Set<StationNode> getNodes() {
        return nodes;
    }

    public List<LocalizedAlert> getAlerts() {
        return alerts;
    }

    public String getStationName() {
        return stationName;
    }

    public Set<StationLink> getLinks() { return this.links; }

    public StationConnectivityResult(TransitStop initialStop, Set<Vertex> vertices, Set<Vertex> accessibles,
                                     List<Alert> alerts, Graph graph, State state, Set<PathwayEdge> pathways,
                                     boolean earlyReturn) {
        this.stationName = initialStop.getName();
        this.stationId = initialStop.getStop().getParentStation();

        this.nodes = new HashSet<>();
        for (Vertex v: vertices) {
            nodes.add(new StationNode(v, accessibles.contains(v)));
        }
        this.alerts = new ArrayList<>();
        alerts.forEach( (alert) -> this.alerts.add(new LocalizedAlert(alert, Locale.US)) );

        this.links = new HashSet<>();
        for (PathwayEdge e: pathways) {
            boolean isActive = e.getElevatorIsOutOfServiceAlerts(graph, state).isEmpty();

            links.add(new StationLink(e.getId(), e.getFromVertex().getIndex(), e.getToVertex().getIndex(),
                    e.getPathwayMode(), e.getPathwayCode(), isActive));
        }
    }

}
