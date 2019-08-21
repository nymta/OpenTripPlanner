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
package org.opentripplanner.routing.comparator;

import org.opentripplanner.routing.spt.GraphPath;

import java.util.Comparator;

public class TransfersComparator implements Comparator<GraphPath> {

    private WalkingComparator walkingComparator = new WalkingComparator();

    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        // if one path has no transfers, compare walking distance
        if (o1.getTrips().isEmpty() || o2.getTrips().isEmpty()) {
            return walkingComparator.compare(o1, o2);
        }
        return o1.getTrips().size() - o2.getTrips().size();
    }
}
