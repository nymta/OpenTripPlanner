package org.opentripplanner.ext.transitlink_window.search;



import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StationStopEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Set;

public class TimeWindowSkipTraverseResultStrategy implements SkipTraverseResultStrategy {

    @Override
    public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent, State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
//      Skip the state if the state time is greater than the end of the time bound.
        //TODO this could be the wrong time to compare.
        if(current.getActiveTime() > parent.getActiveTime())
        {
            return true;
        }
//      Skip the state if the state represents boarding a TripPattern with the same stopping pattern up to the destination as a TripPattern which has been boarded previously, in the same graph path
        //TODO I really don't think this is the right step.
        if(current.getLastPattern().equals(parent.getLastPattern())) {
            return true;
        }

        if(current.getBackEdge() instanceof StationStopEdge) {
            return  true;
        }



        return false;
    }
}