package org.opentripplanner.ext.transitlink_window.search;



import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StationStopEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.lang.reflect.Array;
import java.util.ArrayList;
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
        ArrayList<State> allCurrentBackStates = getAllBackStates(current, new ArrayList<State>());
        ArrayList<State> allParentBackStates = getAllBackStates(parent, new ArrayList<State>());

        boolean shouldSkipTraversalResult = false;

        if(allCurrentBackStates.size() == allParentBackStates.size() && allParentBackStates.size() > 0) {
            for(int i =0; i < allCurrentBackStates.size(); i++){
                //It will not enqueue a state if the state represents a boarding TripPattern with the same stopping pattern up to the destination as a TripPattern that has been boarded previously, in the same graph path.
                if(allCurrentBackStates.get(i).equals(allParentBackStates.get(i))) {
                    shouldSkipTraversalResult = true;
                }

            }
        }

        return shouldSkipTraversalResult;
    }

    private ArrayList<State> getAllBackStates(State state, ArrayList<State> statesList){
        State backState = state.getBackState();
        if (backState != null) {
            statesList.add(backState);
            return getAllBackStates(state, statesList);
        }

        return statesList;
    }
}