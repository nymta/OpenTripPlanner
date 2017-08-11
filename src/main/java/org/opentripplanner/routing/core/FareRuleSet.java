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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;

public class FareRuleSet implements Serializable {

    /*
     * A FareRuleClause is a row in fare_rules.txt. If all rides match clauses, and set-of-rides
     * match journey parameters, the set matches the FareRuleSet.
     */

    private static final long serialVersionUID = 7218355718876553029L;

    private FareAttribute attribute;
    private String agency = null;
    private List<FareRuleClause> clauses;

    private transient Map<ContainsKey, FareRuleClause> rulesByContainsKey;

    public FareRuleSet(FareAttribute attribute) {
        this.attribute = attribute;
        this.clauses = new ArrayList<>();
        this.rulesByContainsKey = Maps.newHashMap();
    }

    public void addFareRule(FareRule rule) {
        if (rule.getContainsId() != null) {
            ContainsKey key = new ContainsKey(rule);
            FareRuleClause clause = rulesByContainsKey.get(key);
            if (clause != null) {
                clause.addContains(rule.getContainsId());
            } else {
                clause = new FareRuleClause(rule);
                clauses.add(clause);
                rulesByContainsKey.put(key, clause);
            }
        } else {
            clauses.add(new FareRuleClause(rule));
        }
    }

    public void addTrip(AgencyAndId trip) {
        clauses.add(new FareRuleClause(trip));
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    /**
     * Return true if given set of rides matches this set of rules.
     *
     * For a set of rides, we need to check
     * (a) clauses (ie rows in fare_rules.txt [multiple rows make up a clause for `contains']).
            - either one clause satisfies the whole sequence, or one clause satisfies each ride.
     * (b) "all ride" properties (agency)
     * (c) whole ride-set properties (transfer time)
     */
    public boolean matches(RideSequence rideSequence) {

        List<Ride> rides = rideSequence.getRides();

        // Must satisfy clauses
        if (!clauses.isEmpty()) {

            // Check if there's a clause that matches the whole sequence
            boolean matches = false;
            for (FareRuleClause clause : clauses) {
                if (clause.matches(rideSequence)) {
                    matches = true;
                    break;
                }
            }

            if (!matches) {
                // Check that each ride matches a clause
                for (Ride ride : rides) {
                    if (!rideMatchesAnyClause(ride)) {
                        return false;
                    }
                }
            }
        }

        // Check all rides match clause (just agency)
        if (agency != null) {
            for (Ride ride : rides) {
                if (!(agency.equals(ride.agency))) {
                    return false;
                }
            }
        }

        // Check that the whole set of rides satisfy the FareAttribute properties
        if (attribute.isTransfersSet() && attribute.getTransfers() < rideSequence.getTransfersUsed()) {
            return false;
        }
        // assume transfers are evaluated at boarding time,
        // as trimet does
        if (attribute.isTransferDurationSet() &&
                rideSequence.getTripTime() > attribute.getTransferDuration()) {
            return false;
        }
        if (attribute.isJourneyDurationSet() &&
                rideSequence.getJourneyTime() > attribute.getJourneyDuration()) {
            return false;
        }

        return true;
    }

    private boolean rideMatchesAnyClause(Ride ride) {
        for (FareRuleClause clause : clauses) {
            if (clause.matches(ride)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAgencyDefined() {
        return agency != null;
    }

    public FareAttribute getFareAttribute() {
        return attribute;
    }
}

class FareRuleClause implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private AgencyAndId route;
    private String origin;
    private String destination;
    private List<String> contains;
    private AgencyAndId trip;

    FareRuleClause(FareRule rule) {
        if (rule.getRoute() != null) {
            this.route = rule.getRoute().getId();
        }
        this.origin = rule.getOriginId();
        this.destination = rule.getDestinationId();
        if (rule.getContainsId() != null) {
            this.contains = new ArrayList<>();
            this.contains.add(rule.getContainsId());
        }
    }

    FareRuleClause(AgencyAndId trip) {
        this.trip = trip;
    }

    void addContains(String zone) {
        this.contains.add(zone);
    }

    /**
     * Check this clause matches an entire sequence
     */
    public boolean matches(RideSequence sequence) {
        // can only match legs separately if this is a route- or trip-limited clause.
        if (route != null || trip != null) {
            return false;
        }

        if (origin != null) {
            if (!origin.equals(sequence.getStartZone())) {
                return false;
            }
        }

        if (destination != null) {
            if (!destination.equals(sequence.getEndZone())) {
                return false;
            }
        }

        if (contains != null) {
            List<Ride> rides = sequence.getRides();
            Set<String> allRideZones = new HashSet<>();
            for (Ride ride : rides) {
                allRideZones.addAll(ride.zones);
            }
            for (String zone : contains) {
                if (!allRideZones.contains(zone)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check that this clause matches a single ride
     */
    public boolean matches(Ride ride) {

        // check for matching origin, if applicable
        if (this.origin != null) {
            if (!this.origin.equals(ride.startZone)) {
                return false;
            }
        }

        // check for matching destination, if applicable
        if (this.destination != null) {
            if (!this.destination.equals(ride.endZone)) {
                return false;
            }
        }

        //check for matching routes
        if (this.route != null) {
            if (!this.route.equals(ride.route)) {
                return false;
            }
        }

        //check for matching trips
        if (this.trip != null) {
            if (!this.trip.equals(ride.trip)) {
                return false;
            }
        }

        // check for matching contains
        if (this.contains != null) {
            for (String zone : this.contains) {
                if (!ride.zones.contains(zone)) {
                    return false;
                }
            }
        }

        return true;
    }
}

// This allows us to add new contains rows to the correct clause
class ContainsKey {
    private AgencyAndId route;
    private String origin;
    private String destination;

    ContainsKey(FareRule rule) {
        if (rule.getRoute() != null) {
            route = rule.getRoute().getId();
        }
        origin = rule.getOriginId();
        destination = rule.getDestinationId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainsKey that = (ContainsKey) o;

        if (route != null ? !route.equals(that.route) : that.route != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        return destination != null ? destination.equals(that.destination) : that.destination == null;
    }

    @Override
    public int hashCode() {
        int result = route != null ? route.hashCode() : 0;
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }
}