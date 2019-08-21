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

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * Models a node internal to a station.
 */
public class StationNode {
    public enum Type { STOP, STATION, ENTRANCE, OTHER, MEZZANINE }

    private int id;
    private String label;
    private double lat;
    private double lon;
    private Type type;
    private long osmWayId;
    private boolean isAccessible;

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public Type getType() {
        return type;
    }

    public long getOsmWayId() {
        return osmWayId;
    }

    public boolean isAccessible() {
        return isAccessible;
    }

    public StationNode(int id, boolean isAccessible) {
        this.id = id;
        this.isAccessible = isAccessible;
    }

    public StationNode(int id, String label, double lat, double lon) {
        this.id = id;
        this.label = label;
        this.lat = lat;
        this.lon = lon;
    }

    public StationNode(Vertex v, boolean isAccessible) {
        this(v.getIndex(), v.getLabel(), v.getLat(), v.getLon());

        Type type = Type.OTHER;
        long wayId = 0;

        if (v instanceof TransitStop) {
            TransitStop tstop = (TransitStop) v;

            if (tstop.isEntrance()) {
                type = Type.ENTRANCE;
            } else {
                switch (tstop.getStop().getLocationType()) {
                    case 0: type = Type.STOP;
                        break;
                    case 1: type = Type.STATION;
                        break;
                    case 2: type = Type.ENTRANCE;
                    case 3: case 4: default: type = Type.OTHER;
                }
            }
            wayId = tstop.getOsmWay();
        }
        this.type = type;
        this.osmWayId = wayId;
        this.isAccessible = isAccessible;
    }
}
