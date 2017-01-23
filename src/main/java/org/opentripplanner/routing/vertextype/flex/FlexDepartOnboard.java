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

package org.opentripplanner.routing.vertextype.flex;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class FlexDepartOnboard extends FlexVertex {

    private static final long serialVersionUID = 1L;

    /** constructor for table trip patterns */
    public FlexDepartOnboard(Graph g, StreetVertex connection, Stop stop, PatternHop hop) {
        super(g, connection, stop, "flexDepart " + hop.toString() + " ");
    }
    
}
