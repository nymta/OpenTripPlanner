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

import org.opentripplanner.routing.edgetype.PathwayEdge;

/**
 * Models a link internal to a station.
 */
public class StationLink {
    public enum Type { OTHER, WALKWAY, STAIRS, MOVING_SIDEWALK, ESCALATOR, ELEVATOR}

    private int id;
    private int sourceId;
    private int destinationId;
    private Type type;
    private String pathwayCode;
    private boolean isActive;

    public int getId() { return this.id; }

    public int getSourceId() { return this.sourceId; }

    public int getDestinationId() { return this.destinationId; }

    public Type getType() { return this.type; }

    public String getPathwayCode() { return this.pathwayCode; }

    public boolean isActive() { return this.isActive; }

    public StationLink(int id, int sourceId, int destinationId, PathwayEdge.Mode mode, String pathwayCode, boolean isActive) {
        this.id = id;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        switch (mode) {
            case WALKWAY: this.type = Type.WALKWAY;
                break;
            case STAIRS: this.type = Type.STAIRS;
                break;
            case ELEVATOR: this.type = Type.ELEVATOR;
                break;
            default: this.type = Type.OTHER;
        }
        this.type = type;
        this.pathwayCode = pathwayCode;
        this.isActive = isActive;
    }
}
