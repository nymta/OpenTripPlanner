
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
package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.ignore.PathIgnoreStrategy;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTDPathIgnoreStrategy implements PathIgnoreStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RTDPathIgnoreStrategy.class);

    @Override
    public boolean shouldIgnorePath(GraphPath path) {
        RoutingRequest options = path.states.getFirst().getOptions();
        return (graphPathStartsLaterThanLimit(path, options.tripShownRangeTime, options) || graphPathExceedsMaxTransferTime(path, options));
    }

    private static boolean graphPathStartsLaterThanLimit(GraphPath path, int range, RoutingRequest options) {
        boolean result = false;
        if (options.arriveBy) {
            long arrivetime = options.dateTime;
            if (arrivetime - path.getEndTime() > range) {
                result = true;
            }
        } else {
            long startTime = path.getStartTime();
            long departtime = options.dateTime;
            if (startTime - departtime > range) {
                result = true;
            }
        }
        return result;
    }

    private static boolean graphPathExceedsMaxTransferTime(GraphPath path, RoutingRequest options) {

        long lastTransitDeparture = -1;

        State[] states = path.states.toArray(new State[path.states.size()]);

        for (int i = 1; i < states.length; i++) {
            if (states[i].getBackMode() == null || !states[i].getBackMode().isTransit()) {
                continue;
            }

            // If it is transit, check if transfer time is too long. Need to check LAST state because
            // this state is *after* a PatternHop.
            long transferTime = states[i - 1].getTimeSeconds() - lastTransitDeparture;
            if (lastTransitDeparture > 0 && (transferTime > options.maxTransferTime || transferTime < options.minTransferTimeHard)) {
                LOG.debug("for itinerary {}, transfer time {} is not in range", path.getTrips(), transferTime);
                return true;
            }

            while (states[i].getBackMode() != null && states[i].getBackMode().isTransit()) {
                i++;
            }

            if (i < states.length) {
                lastTransitDeparture = states[i - 1].getTimeSeconds();
            }
        }

        return false;
    }

}