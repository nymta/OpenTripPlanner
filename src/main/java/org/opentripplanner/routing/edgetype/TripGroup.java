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
package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

public class TripGroup {

    public TripGroup(TripTimes t) {
        trips = new TreeSet<>(Comparator.comparingInt(tt -> tt.getDepartureTime(0)));
        trips.add(t);
        minHeadway = Integer.MAX_VALUE;
        maxHeadway = Integer.MIN_VALUE;
        avgHeadway = 0;
        headways = new ArrayList<>();
    }

    private SortedSet<TripTimes> trips; // sorted, size > 2
    // headways are relative to first stop
    private List<Integer> headways;
    private int minHeadway;
    private int maxHeadway;
    private double avgHeadway;
    // required that trip is after all trip in trips
    // return true if added, false if not.

    public boolean addTrip(TripTimes tt) {
        TripTimes last = trips.last();
        int headway = tt.getDepartureTime(0) - last.getDepartureTime(0);
        double avg = ((avgHeadway * trips.size()) + headway) / (trips.size() + 1.0);
        double maxError = Stream.concat(headways.stream(), Stream.of(headway))
                .mapToDouble(h -> Math.abs(h - avg))
                .max().getAsDouble();
        if (maxError > 300)
            return false;
        headways.add(headway);
        minHeadway = Math.min(minHeadway, headway);
        maxHeadway = Math.max(maxHeadway, headway);
        avgHeadway = avg;
        trips.add(tt);
        return true;
    }

    public int getEstimatedWait() {
        return (int) Math.round(avgHeadway / 2);
    }

    public int getMinTime(int stopIndex) {
        return trips.first().getDepartureTime(stopIndex);
    }

    public int getMaxTime(int stopIndex) {
        return trips.last().getArrivalTime(stopIndex);
    }

    public TripTimes getExemplar() {
        return trips.first();
    }
}