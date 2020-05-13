package org.opentripplanner.ext.transitlink_window.search;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.spt.DominanceFunction;

import java.util.Objects;

public class TimeWindowDominanceFunction extends DominanceFunction {

    @Override
    protected boolean betterOrEqual(State a, State b) { return a.weight <= b.weight; }

    @Override
    public boolean betterOrEqualAndComparable(State a, State b) {

//      First check  if the lists of trips which have been boarded are identical, or if transit has not been used in either state
        if(a.trip)

        // States before boarding transit and after riding transit are incomparable.
        // This allows returning transit options even when walking to the destination is the optimal strategy.
        if (a.isEverBoarded() != b.isEverBoarded()) {
            return false;
        }

        // The result of a SimpleTransfer must not block alighting normally from transit. States that are results of
        // SimpleTransfers are incomparable with states that are not the result of SimpleTransfers.
        if ((a.backEdge instanceof SimpleTransfer) != (b.backEdge instanceof SimpleTransfer)) {
            return false;
        }

        // A TimedTransferEdge might be invalidated later, when we have boarded the next trip and have all the information
        // we need to check the specificity. We do not want states that might be invalidated to dominate other valid
        // states.
        if ((a.backEdge instanceof TimedTransferEdge) || (b.backEdge instanceof TimedTransferEdge)) {
            return false;
        }

        // Does one state represent riding a rented bike and the other represent walking before/after rental?
        if (a.isBikeRenting() != b.isBikeRenting()) {
            return false;
        }

        // In case of bike renting, different networks (ie incompatible bikes) are not comparable
        if (a.isBikeRenting()) {
            if (!Objects.equals(a.getBikeRentalNetworks(), b.getBikeRentalNetworks()))
                return false;
        }

        // Does one state represent driving a car and the other represent walking after the car was parked?
        if (a.isCarParked() != b.isCarParked()) {
            return false;
        }

        // Does one state represent riding a bike and the other represent walking after the bike was parked?
        if (a.isBikeParked() != b.isBikeParked()) {
            return false;
        }

        // Are the two states arriving at a vertex from two different directions where turn restrictions apply?
        if (a.backEdge != b.getBackEdge() && (a.backEdge instanceof StreetEdge)) {
            if (! a.getOptions().getRoutingContext().graph.getTurnRestrictions(a.backEdge).isEmpty()) {
                return false;
            }
        }

        // These two states are comparable (they are on the same "plane" or "copy" of the graph).
        return betterOrEqual(a, b);

    }
}


//public static class MinimumWeight extends DominanceFunction {
//    /** Return true if the first state has lower weight than the second state. */
//    @Override
//    public boolean betterOrEqual (State a, State b) { return a.weight <= b.weight; }
//}
//
///**
// * This approach is more coherent in Analyst when we are extracting travel times from the optimal
// * paths. It also leads to less branching and faster response times when building large shortest path trees.
// */
//public static class EarliestArrival extends DominanceFunction {
//    /** Return true if the first state has lower elapsed time than the second state. */
//    @Override
//    public boolean betterOrEqual (State a, State b) { return a.getElapsedTimeSeconds() <= b.getElapsedTimeSeconds(); }
//}