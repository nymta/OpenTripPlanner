package org.opentripplanner.ext.transitlink_window.search;

import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class FareEntryRestrictionsSkipEdgeStrategy implements SkipEdgeStrategy {


    @Override
    public boolean shouldSkipEdge(Vertex origin, Vertex target, State current, Edge edge, ShortestPathTree spt, RoutingRequest traverseOptions) {

       	// Do not process an edge if it represents the first edge traversal and it is NOT an edge into fare control, if the firstTraversalIntoFareEntry constraint is active.
        if(traverseOptions.firstTraversalIntoFareEntry && origin == null){
            return !InstanceOfStreetVertex(target);
        }
        //Do not process an edge if it represents an edge out of fare control and the to-vertex is not the destination of the trip plan, if the fareExitOnlyToDestination constraint is active.
        if(traverseOptions.fareExitOnlyToDestination  && target == null){
            return !InstanceOfStreetVertex(origin);
        }

        //Do not process an edge if it is a street edge and exclude street edges is active.
        if (edge instanceof TransferEdge || edge instanceof PathwayEdge)
        {
            if(traverseOptions.excludeStreetEdges){
                if(InstanceOfStreetVertex(target)){
                    return true;
                }
            }
        }
        else {
            return false;
        }

        return false;
    }

    //Given a vertex it will determine if the vertex is linked to a street vertex which would do not want to explore.
    private boolean InstanceOfStreetVertex(Vertex v) {
        return (v instanceof BikeParkVertex || v instanceof BikeRentalStationVertex || v instanceof ParkAndRideVertex || v instanceof StreetVertex);
    }
}
