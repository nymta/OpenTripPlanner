package org.opentripplanner.ext.transitlink_window.search;

import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class TimeWindowSearch extends GenericDijkstra {

    private RoutingRequest options;

    public TimeWindowSearch(RoutingRequest options) {
        super(options);
        this.options = options;
    }

    public ShortestPathTree findShortestPathTree(State requestState) {
        super.setSkipEdgeStrategy(new FareEntryRestrictionsSkipEdgeStrategy());
//        super.setSkipTraverseResultStrategy(new TimeWindowSkipTraverseResultStrategy());


        return super.getShortestPathTree(requestState);
    }

    @Override
    public ShortestPathTree getShortestPathTreeFromDominanceFunction() {
//        return new DominanceFunction.MinimumWeight().getNewShortestPathTree(options);
        return new TimeWindowDominanceFunction().getNewShortestPathTree(options);
    }

}
