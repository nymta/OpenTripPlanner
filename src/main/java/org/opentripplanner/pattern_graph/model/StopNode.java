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
package org.opentripplanner.pattern_graph.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.model.StopShort;

import java.util.HashSet;
import java.util.Set;

public class StopNode {

    private StopShort stop;

    private Set<AgencyAndId> successors = new HashSet<>();

    private AgencyAndId stopId;

    public StopNode(AgencyAndId stopId) {
        this.stopId = stopId;
    }

    public StopShort getStop() {
        return stop;
    }

    public void setStop(StopShort stop) {
        this.stop = stop;
    }

    public Set<AgencyAndId> getSuccessors() {
        return successors;
    }

    public AgencyAndId getStopId() {
        return stopId;
    }

    public void addSuccessor(StopNode node) {
        successors.add(node.getStopId());
    }
}
