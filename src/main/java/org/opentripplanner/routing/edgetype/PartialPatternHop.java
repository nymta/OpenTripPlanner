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

package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.module.map.StreetMatcher;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

import java.util.ArrayList;
import java.util.List;


public class PartialPatternHop extends PatternHop {

    private static final long serialVersionUID = 1L;

    private double percentageOfHop;

    public PartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), hop.getContinuousPickup(), hop.getContinuousDropoff(), false);
        this.percentageOfHop = calculatePercentageOfHop(hop);
    }

    public PartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, StreetMatcher matcher, GeometryFactory factory) {
        this(hop, from, to, fromStop, toStop);
        setGeometryFromHop(matcher, factory, hop);
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return percentageOfHop * super.timeLowerBound(options);
    }

    @Override
    public int getRunningTime(State s0) {
        return (int) (percentageOfHop * super.getRunningTime(s0));
    }

    private void setGeometryFromHop(StreetMatcher matcher, GeometryFactory factory, PatternHop hop) {
        List<Edge> edges = matcher.match(hop.getGeometry());
        List<Coordinate> coords = new ArrayList<>();
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        //boolean fromMatch = hop.getFromVertex().equals(getFromVertex());
        for (Edge e : edges) {
            double pct = line.project(e.getToVertex().getCoordinate())/line.getEndIndex();
            if (pct >= percentageOfHop)
                break;
            for (Coordinate c : e.getGeometry().getCoordinates())
                coords.add(c);
        }
        Coordinate[] arr = coords.toArray(new Coordinate[0]);
        LineString geometry = factory.createLineString(arr);
        setGeometry(geometry);
    }

    private double calculatePercentageOfHop(PatternHop hop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        double idx = line.project(getToVertex().getCoordinate());
        return idx/line.getEndIndex();
    }

}

