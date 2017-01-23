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

package org.opentripplanner.routing.edgetype.flex;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.flex.FlexDepartOnboard;
import org.opentripplanner.routing.vertextype.flex.FlexStopDepart;

public class FlexTransitBoardAlight extends TransitBoardAlight {

    private TripPattern pattern;
    private PatternHop hop;

    public FlexTransitBoardAlight(FlexStopDepart from, FlexDepartOnboard to, TripPattern pattern, PatternHop hop) {
        super(from, to, hop.getStopIndex(), pattern.mode, true);
        this.pattern = pattern;
        this.hop = hop;
    }

    // TODO: override stuff

   /* @Override
    public int possibleAddWaitTime(int waitTimes, TripTimes tripTimes) {

        // for now, very naive, take percentage of spherical distance and apply to time. TODO: make it smarter.
        double totalHopDistance = hop.getDistance();
        double stopToFlexDistance = SphericalDistanceLibrary.distance(getStop().getLat(), getStop().getLon(), getFromVertex().getLat(),
                getFromVertex().getLon());

        double totalTime = tripTimes.getRunningTime(getStopIndex());

        double flexTime = (stopToFlexDistance / totalHopDistance) * totalTime;

        return waitTimes + (int) flexTime;
    }*/

    @Override
    public TripPattern getPattern() {
        return pattern;
    }

    @Override
    protected Stop getStop() {
        FlexDepartOnboard stopVertex = (FlexDepartOnboard) (boarding ? tov : fromv);
        return stopVertex.getStop();
    }
}
