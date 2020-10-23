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

import org.onebusaway.gtfs.model.Stop;
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
    	// all MTA buses are accessible
    	if(stop.getStopId().getAgencyId().equals("MTA NYCT") || stop.getStopId().getAgencyId().equals("MTABC"))
    		return AccessibilityResult.ALWAYS_ACCESSIBLE;
    	return stop.hasWheelchairEntrance() ? AccessibilityResult.ALWAYS_ACCESSIBLE : AccessibilityResult.NEVER_ACCESSIBLE;
    }

    @Override
    protected boolean canUsePathway(State state, PathwayEdge pathway, List<Alert> alerts) {
    	return pathway.isWheelchairAccessible();
    }
    
    @Override
    public boolean transitStopEvaluateGTFSAccessibilityFlag(Stop s) {
		return s.getWheelchairBoarding() == 2;
    }

}
