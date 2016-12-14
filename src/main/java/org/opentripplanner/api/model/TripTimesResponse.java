package org.opentripplanner.api.model;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.model.TripTimeShort;

public class TripTimesResponse {

	AgencyAndId tripId;
	
	List<TripTimeShort> stopTimes;
	
	public TripTimesResponse(AgencyAndId tripId, List<TripTimeShort> stopTimes) {
		this.tripId = tripId;
		this.stopTimes = stopTimes;
	}
	
	public AgencyAndId getTripId() {
		return tripId;
	}

	public void setTripId(AgencyAndId tripId) {
		this.tripId = tripId;
	}

	public List<TripTimeShort> getStopTimes() {
		return stopTimes;
	}

	public void setStopTimes(List<TripTimeShort> stopTimes) {
		this.stopTimes = stopTimes;
	}
	
}
