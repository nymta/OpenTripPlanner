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
        if(current.getTimeSeconds() > traverseOptions.secondDateTime)
        {
            return true;
        }

        //It will not enqueue a state if the state represents a boarding TripPattern with the same stopping pattern up to the destination as a TripPattern that has been boarded previously, in the same graph path.
        //TODO I really don't think this is the right step.
//        if(current.getLastPattern() != null && parent.getLastPattern() != null && current.getLastPattern().equals(parent.getLastPattern())) {
//            return true;
//        }

        //if the state represents a station that has been visited previously in the same graph path, this is to prevent backtracking through the system.
//        if( (current.isEverBoarded() && current.b) && (parent.isEverBoarded() && ) ) {
//            return true;
//        }

        return false;
    }
}