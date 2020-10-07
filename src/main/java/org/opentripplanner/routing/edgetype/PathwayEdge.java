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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Pathway;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge {

    public enum Mode { NONE, WALKWAY, STAIRS, MOVING_SIDEWALK, ESCALATOR, ELEVATOR, FARE_GATE, EXIT_GATE }

    private AgencyAndId id = null;
    
    private int traversalTime;

    private double maxSlope;
    
    private int stairCount;
    
    private double minWidth;
    
    private Mode pathwayMode = Mode.NONE;

    private double length;

    private boolean verbose = false;
    
    private int mtaIsAccessible;
    
    private static final Logger LOG = LoggerFactory.getLogger(PathwayEdge.class);

    public PathwayEdge(AgencyAndId id, Vertex fromv, Vertex tov, double length, int pathwayMode, int traversalTime, 
    		double minWidth, double maxSlope, int stairCount, int isAccessible) {
        super(fromv, tov);
        this.id = id;
        this.pathwayMode = Mode.values()[pathwayMode];
        this.minWidth = minWidth;
        this.maxSlope = maxSlope;
        this.stairCount = stairCount;
        this.traversalTime = traversalTime;
        this.length = length;
        this.mtaIsAccessible = isAccessible;

        // set some defaults
        if(this.stairCount > 0 && this.traversalTime == 0) {
        	this.traversalTime = this.stairCount * 5; // 5s per stair
        }
        
        if(this.length > 0 && this.traversalTime == 0) {
        	this.traversalTime = (int)(this.length * 1.4); // average walk speed: 1.4 m/s
        }
    }

    private static final long serialVersionUID = -3311099256178798981L;

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return length;
    }
    
    public AgencyAndId getPathwayId() {
    	return this.id;
    }
    
    public TraverseMode getMode() {
       return TraverseMode.WALK;
    }

    public Mode getPathwayMode() { return this.pathwayMode; }

    @Override
    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { getFromVertex().getCoordinate(),
                getToVertex().getCoordinate() };
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    @Override
    public boolean isApproximateGeometry() {
        return true;
    }

    public String getName() {
        switch(pathwayMode) {
        	case MOVING_SIDEWALK:
        	case ESCALATOR:
        		return "escalator (" + this.getPathwayId() + ")";
            case ELEVATOR:
                return "elevator (" + this.getPathwayId() + ")";
            case STAIRS:
                return "stairs (" + this.getPathwayId() + ")";
            case WALKWAY:
                return "walkway (" + this.getPathwayId() + ")";
            case FARE_GATE:
            	return "fare gate (" + this.getPathwayId() + ")";
            case EXIT_GATE:
            	return "exit gate (" + this.getPathwayId() + ")";
            default:
                return "pathway (" + this.getPathwayId() + ")";
        }
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }
    
    public boolean isElevator() {
        return Mode.ELEVATOR.equals(pathwayMode);
    }

    public boolean hasDefinedMode() {
        return !pathwayMode.equals(Mode.NONE);
    }

    @Override
    public boolean isWheelchairAccessible() {
    	if(this.mtaIsAccessible == 1) // MTA asserts path is accessible
    		return true;
    	else { // go by heuristic:
    		if(this.pathwayMode == Mode.STAIRS || this.pathwayMode == Mode.ESCALATOR || this.pathwayMode == Mode.MOVING_SIDEWALK 
    				|| this.pathwayMode == Mode.MOVING_SIDEWALK)
    			return false;

    		// from https://developers.google.com/transit/gtfs/reference#pathwaystxt
    		if(this.maxSlope > .083) 
    			return false;

    		// from https://developers.google.com/transit/gtfs/reference#pathwaystxt
    		if(this.minWidth < 1) 
    			return false;

    		return true;
    	}
    }

    public State traverse(State s0) {
        verbose = false;
        int time = this.traversalTime;
        
        if (s0.getOptions().wheelchairAccessible) {
            if (!isWheelchairAccessible() ||
                    (!s0.getOptions().ignoreRealtimeUpdates && pathwayMode.equals(Mode.ELEVATOR) && elevatorIsOutOfService(s0))) {
                if (verbose) {
                    System.out.println("   wheelchairAccessible == true AND elevatorIsOutOfService == true");
                    LOG.info("   debug disallow, wheelchairAccessible == true AND elevatorIsOutOfService == true");
                }

                return null;
            }
        }
        
        StateEditor s1 = s0.edit(this);
        // Allow transfers to the street if the PathwayEdge is proceeded by a TransferEdge
        if (s0.backEdge instanceof TransferEdge) {
            s1.setTransferPermissible();
        }
        
        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(time);
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    private boolean elevatorIsOutOfService(State s0) {
        List<Alert> alerts = getElevatorIsOutOfServiceAlerts(s0.getOptions().rctx.graph, s0);
        return !alerts.isEmpty();
    }

    public List<Alert> getElevatorIsOutOfServiceAlerts(Graph graph, State s0) {
        List<Alert> alerts = new ArrayList<>();
        for (AlertPatch alert : graph.getAlertPatches(this)) {
            if (alert.displayDuring(s0) && alert.getElevatorId() != null && pathwayMode == Mode.ELEVATOR) {
                alerts.add(alert.getAlert());
            }
        }
        return alerts;
    }
}
