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
import org.opentripplanner.pattern_graph.model.StopNodeAttribute;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.profile.StopCluster;

import java.util.HashSet;
import java.util.Set;

public class StopNode {

    private StopShort attributes;

    private StopNodeAttribute nodeAttribute;

    private Set<String> successors = new HashSet<>();

    private String stopId;

    public StopNode(String stopId) {
        this.stopId = stopId;
    }

    public StopShort getAttributes() {
        return attributes;
    }

    public void setAttributes(StopShort attributes) {
        this.attributes = attributes;
    }

    public Set<String> getSuccessors() {
        return successors;
    }

    public String getStopId() {
        return stopId;
    }

    public void addSuccessor(StopNode node) {
        successors.add(node.getStopId());
    }

    public StopNodeAttribute getNodeAttribute() {
        return nodeAttribute;
    }

    public void setNodeAttribute(StopNodeAttribute nodeAttribute) {
        this.nodeAttribute = nodeAttribute;
    }
}
