package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.flex.FlexDepartOnboard;

/** Models going from flex alighting to the next pattern arrival. */
public class FlexHop extends PatternHop {

    public FlexHop(FlexDepartOnboard from, PatternArriveVertex to, int stopIndex) {
        super(from, to, from.getStop(), to.getStop(), stopIndex);
    }

    // TODO: overwrite stuff.

    @Override
    public TripPattern getPattern() {
        return ((PatternArriveVertex) getToVertex()).getTripPattern();
    }
}
