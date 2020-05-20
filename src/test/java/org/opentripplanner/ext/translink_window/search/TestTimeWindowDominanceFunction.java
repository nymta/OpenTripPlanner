package org.opentripplanner.ext.translink_window.search;

import junit.framework.TestCase;
import org.junit.Assert;
import org.opentripplanner.ext.transitlink_window.search.TimeWindowDominanceFunction;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.SampleEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

import static org.mockito.Mockito.mock;

public class TestTimeWindowDominanceFunction extends TestCase {

    public void testTimedWindowsDominanceFunction() {
        TimeWindowDominanceFunction timeWindowDominanceFunction = new TimeWindowDominanceFunction();
        Vertex fromVertex = mock(TransitStopArrive.class);
        Vertex toVertex = mock(TransitStopDepart.class);
        RoutingRequest request = new RoutingRequest();

        // Test if domination works in the general case

        State stateA = new State(fromVertex, null, 0, request);
        State stateB = new State(toVertex, null, 0, request);
        stateA.weight = 1;
        stateB.weight = 2;

        Assert.assertTrue(timeWindowDominanceFunction.betterOrEqualAndComparable(stateA, stateB));
        Assert.assertFalse(timeWindowDominanceFunction.betterOrEqualAndComparable(stateB, stateA));

        // Test different back edges
//        SampleEdge testEdge = new SampleEdge(fromVertex, toVertex, 32);

        State stateC = new State(fromVertex, null, 0, request);
        State stateD = new State(toVertex, null, 0, request);
        stateC.weight = 1;
        stateD.weight = 2;

        Assert.assertFalse(timeWindowDominanceFunction.betterOrEqualAndComparable(stateC, stateD));

        // Test same back edges
        State stateE = new State(fromVertex, null, 0, request);
        State stateF = new State(toVertex, null, 0, request);
        stateE.weight = 1;
        stateF.weight = 2;

        Assert.assertFalse(timeWindowDominanceFunction.betterOrEqualAndComparable(stateE, stateF));

    }

}
