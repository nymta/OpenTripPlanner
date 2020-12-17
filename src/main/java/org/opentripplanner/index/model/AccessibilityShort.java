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
package org.opentripplanner.index.model;

import java.io.Serializable;
import java.util.ArrayList;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.module.RouteStopsAccessibilityTaggerModule.ADAFlag;
import org.opentripplanner.graph_builder.module.RouteStopsAccessibilityTaggerModule.RouteStopTag;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;

public class AccessibilityShort extends StopShort {

	public class RouteShortWrapper extends RouteShort implements Serializable {
		
		private static final long serialVersionUID = 3915566835621828907L;

		public RouteShortWrapper(Route route, RouteStopTag tag) {
			super(route);
			this.accessibilityFlag = tag.accessibilityFlag;
			this.accessibilityNotes = tag.accessibilityNotes;
		}

		public ADAFlag accessibilityFlag;
			
		public String accessibilityNotes;
	}
	
	public ArrayList<RouteShortWrapper> service;
	
	public AccessibilityShort(Stop stop, ArrayList<RouteStopTag> tags, Graph graph) {
		super(stop);

		this.service = new ArrayList<RouteShortWrapper>();		
		for(RouteStopTag tag : tags) {
			Route route = graph.index.routeForId.get(GtfsLibrary.convertIdFromString(tag.routeId));
			this.service.add(new RouteShortWrapper(route, tag));
		}
	}
}
