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

package org.opentripplanner.graph_builder.module.flex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.apache.commons.math3.util.Pair;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.extra_graph.EdgesForRoute;
import org.opentripplanner.graph_builder.module.map.StreetMatcher;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.flex.FlexHop;
import org.opentripplanner.routing.edgetype.flex.FlexPreBoardEdge;
import org.opentripplanner.routing.edgetype.flex.FlexTransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.flex.FlexDepartOnboard;
import org.opentripplanner.routing.vertextype.flex.FlexStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Uses the shapes from GTFS to determine which streets buses drive on, and link to TripPattern.
 * This is based on BusRouteStreetMatcher.
 */
public class FlagStopStreetMatcher implements GraphBuilderModule {
    private static final Logger log = LoggerFactory.getLogger(FlagStopStreetMatcher.class);

    public List<String> provides() {
        return Arrays.asList("edge matching");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "transit");
    }

    private Map<StreetVertex, FlexStopDepart> flexStopMap = new HashMap<>();

    private FlexStopDepart getOrCreate(Graph graph, StreetVertex street, Stop previousStop) {
        FlexStopDepart s = flexStopMap.get(street);
        if (s == null) {
            s = new FlexStopDepart(graph, street, previousStop);
            flexStopMap.put(street, s);
            new FlexPreBoardEdge(street, s);
        }
        return s;
    }


    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        //Mapbuilder needs transit index
        if (graph.streetIndex == null) {
            graph.index(new DefaultStreetVertexIndexFactory());
        }

        StreetMatcher matcher = new StreetMatcher(graph);
        log.info("Finding corresponding street edges for trip patterns...");

        for (Route route : graph.index.routeForId.values()) {
            for (TripPattern pattern : graph.index.patternsForRoute.get(route)) {
                if (pattern.mode == TraverseMode.BUS) {
                    /* we can only match geometry to streets on bus routes */
                    log.debug("Matching {}", pattern);
                    if (pattern.geometry == null) {
                        continue;
                    }

                    for (PatternHop hop : pattern.getPatternHops()) {
                        List<Edge> edges = matcher.match(hop.getGeometry());
                        List<Vertex> vertices = edges.stream()
                                .map(e -> Arrays.asList(e.getFromVertex(), e.getToVertex()))
                                .flatMap(List::stream).collect(Collectors.toList());
                        for (Vertex vertex : vertices) {
                            if (vertex instanceof StreetVertex) {
                                StreetVertex street = (StreetVertex) vertex;
                                Stop previousStop = hop.getBeginStop();
                                PatternArriveVertex arrive = (PatternArriveVertex) hop.getToVertex();

                                // FlexStopDepart should be unique to StreetVertex and previousStop. TODO: maybe not previousStop
                                // FlexDepartOnboard should be unique to StreetVertex, previousStop, and hop
                                FlexStopDepart stopDepart = getOrCreate(graph, street, previousStop);
                                FlexDepartOnboard departOnboard = new FlexDepartOnboard(graph, street, previousStop, hop);

                                new FlexTransitBoardAlight(stopDepart, departOnboard, pattern, hop);
                                new FlexHop(departOnboard, arrive, hop.getStopIndex());
                            }
                        }
                    }

                }
            }
        }
    }

    @Override
    public void checkInputs() {
        //no file inputs
    }
}
