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
package org.opentripplanner.routing.core;

import java.util.List;

public class RideSequence {

    // adapted from DefaultFareServiceImpl

    private List<Ride> rides;
    private int transfersUsed;
    private long tripTime;
    private long journeyTime;
    private String startZone;
    private String endZone;

    public RideSequence(List<Ride> rides) {
        this.rides = rides;

        transfersUsed = -1;

        Ride firstRide = rides.get(0);
        long   startTime = firstRide.startTime;
        String startZone = firstRide.startZone;
        String endZone = firstRide.endZone;
        // stops don't really have an agency id, they have the per-feed default id
        long lastRideStartTime = firstRide.startTime;
        long lastRideEndTime = firstRide.endTime;
        for (Ride ride : rides) {
            lastRideStartTime = ride.startTime;
            lastRideEndTime = ride.endTime;
            endZone = ride.endZone;
            transfersUsed += 1;
        }

        tripTime = lastRideStartTime - startTime;
        journeyTime = lastRideEndTime - startTime;
        this.startZone = startZone;
        this.endZone = endZone;
    }

    /**
     * Returns true if this RideSequence includes rides from only one feed
     */
    public boolean isOneFeed() {
        String feedId = getFeedId();
        for (Ride ride : rides) {
            if(!ride.firstStop.getId().getAgencyId().equals(feedId)) {
                return false;
            }
        }
        return true;
    }

    public String getFeedId() {
        return rides.get(0).firstStop.getId().getAgencyId();
    }

    public List<Ride> getRides() {
        return rides;
    }

    public int getTransfersUsed() {
        return transfersUsed;
    }

    public long getTripTime() {
        return tripTime;
    }

    public long getJourneyTime() {
        return journeyTime;
    }

    public String getStartZone() {
        return startZone;
    }

    public String getEndZone() {
        return endZone;
    }
}
