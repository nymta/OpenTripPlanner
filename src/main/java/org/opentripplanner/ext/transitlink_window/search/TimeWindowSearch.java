package org.opentripplanner.ext.transitlink_window.search;

import org.opentripplanner.routing.algorithm.GenericDijkstra;
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
        //TODO create a new custome ckip Edge strategy to populate here.
        super.setSkipEdgeStrategy(super.skipEdgeStrategy);

        return super.getShortestPathTree(requestState);
    }

    @Override
    public ShortestPathTree getShortestPathTreeFromDominanceFunction() {
        return new DominanceFunction.MinimumWeight().getNewShortestPathTree(options);
    }

}
