package org.opentripplanner.api.model;

import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TripTimeShort;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "TripTimeShort")
public class ApiTripTimeShort {
    public static final int UNDEFINED = -1;

    public String stopId;
    public int stopIndex;
    public int stopCount;
    public int scheduledArrival = UNDEFINED ;
    public int scheduledDeparture = UNDEFINED ;
    public int realtimeArrival = UNDEFINED ;
    public int realtimeDeparture = UNDEFINED ;
    public int arrivalDelay = UNDEFINED ;
    public int departureDelay = UNDEFINED ;
    public boolean timepoint = false;
    public boolean realtime = false;
    public ApiRealTimeState realtimeState = ApiRealTimeState.SCHEDULED ;
    public long serviceDay;
    public FeedScopedId tripId;
    public String blockId;
    public String headsign;

    public ApiTripTimeShort(TripTimeShort other) {
        stopId             = FeedScopedIdMapper.mapToApi(other.stopId);
        stopIndex          = other.stopIndex;
        stopCount          = other.stopCount;
        scheduledArrival   = other.scheduledArrival;
        scheduledDeparture = other.scheduledDeparture;
        realtimeArrival    = other.realtimeArrival;
        realtimeDeparture  = other.realtimeDeparture;
        arrivalDelay       = other.arrivalDelay;
        departureDelay     = other.departureDelay;
        timepoint          = other.timepoint;
        realtime           = other.realtime;
        realtimeState      = ApiRealTimeState.RealTimeState(other.realtimeState);
        blockId            = other.blockId;
        headsign           = other.headsign;
    }
}
