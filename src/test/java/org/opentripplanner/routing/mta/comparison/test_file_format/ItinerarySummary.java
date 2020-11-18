package org.opentripplanner.routing.mta.comparison.test_file_format;

import org.opentripplanner.routing.mta.comparison.CompareODResults.platformDim;

public class ItinerarySummary {

	public int itineraryNumber;
	
	public double walkDistance;
	
	public int transitTime;
	
	public String routes = "";
	
	public platformDim platform;

	public String toString() {
		return platform.toString() + "" + itineraryNumber + ": Walk=" + walkDistance + ", transit=" + transitTime + ", Routes = " + routes;
	}
	
	public ItinerarySummary(String line) throws Exception {
		String parts[] = line.split(" ");
		
		if(parts.length < 4 && parts[0].equals("S"))
			throw new Exception("Nope.");
		
		itineraryNumber = Integer.parseInt(parts[1].trim());
		walkDistance = Double.parseDouble(parts[2].trim());
		transitTime = Integer.parseInt(parts[3].trim());

		// some results have no routes
		if(parts.length > 4)
			routes = parts[4].trim();
	}
}