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

import java.util.ArrayList;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.module.RouteStopsAccessibilityTaggerModule.RouteStopTag;
import org.opentripplanner.gtfs.GtfsLibrary;

public class AccessibilityShort {
	
	public String stopId;
	
	public String stopName;

	public ArrayList<RouteStopTag> service;
	
	public AccessibilityShort(Stop stop, ArrayList<RouteStopTag> tags) {
		this.stopId = GtfsLibrary.convertIdToString(stop.getId());
		this.stopName = stop.getName();
		this.service = tags;
	}
}
