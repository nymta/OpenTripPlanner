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
package org.opentripplanner.routing.mta;

import org.junit.Test;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.NonLocalizedString;

import java.util.List;

import static org.junit.Assert.assertFalse;

public class StationToStationRoutingTest extends MTAGraphTest {

    // TODO add tests for other cases - wheelchair accessible/not accessible

    // 1st Av to Bedford
    @Test
    public void testFirstAveToBedford() {
        TransitStop canal = (TransitStop) graph.getVertex("MTASBWY:L06S");
        TemporaryStreetLocation tmpVertex = new TemporaryStreetLocation("tmp", canal.getCoordinate(), new NonLocalizedString("tmp"), false);
        new TemporaryFreeEdge(tmpVertex, canal);
        RoutingRequest options = new RoutingRequest();
        options.wheelchairAccessible = true;
        GraphPath path = search(tmpVertex, graph.getVertex("MTASBWY:L08S"), "2018-03-15", "4:00pm", options);
        List<TestRide> rides = TestRide.createRides(path);
        assertFalse(rides.isEmpty());
        TestRide firstRide = rides.get(0);
        assertFalse(firstRide.getRoute().getId().equals("J"));
    }

}
