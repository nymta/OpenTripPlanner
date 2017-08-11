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

import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;
import org.onebusaway.gtfs.model.Route;

import java.util.Arrays;

public class TestFareRuleSet extends TestCase {

    /*
    A summary of the semantics of fare_rules.txt and fare_attributes.txt is given below. This is
    adapted from Brian Ferris's explanation at the beginning of his fare proposal:
    https://docs.google.com/document/d/1mK3--o5g4-3cCXaqmch92U63JTwChh0L2VCmcDViIlM/

    Definitions:
    - Leg: Travel by a rider on one transit vehicle from a departure stop to an arrival stop. (In
      the OTP fare model, this is a *Ride*).
    - Leg Sequence: Travel by a rider on one or more consecutive transit legs.
    - Itinerary: The complete sequence of legs a rider would take from the start of their trip to the end.

    All legs in an itinerary must be matched to a fare. A fare can match a leg or a leg sequence.

    Fare IDs are defined in fare_attributes.txt. Fare rules are defined in fare_rules.txt. Rules
    match legs in one or more of the following ways, which correspond to columns in fare_rules.txt:
    - route_id: matches a single leg that belongs to the route with the specified id, as matched
      against the route_id column of routes.txt.
    - origin_id: matches one or more legs in sequence whose first leg departs from the specified
      zone, as matched against the zone_id of the departure stop defined in stops.txt.  Typically
      used in combination with destination_id to describe the start and end point for a sequence
      of legs.
    - destination_id: matches one or more legs in sequence whose last leg arrives in the specified
      zone, as matched against the zone_id of the arrival stop defined in stops.txt.  Typically used
      in combination with origin_id to describe the start and end point for a sequence of legs.
    - contains_id: when given, a leg sequence must pass through all zones associated with the fare
      via contains_id.

    Different fare clauses combine with OR, and different columns (route_id, origin_id,
    destination_id, contains_id) combine with AND.

    Note that route_id matches a single leg, and the other rules can match a leg sequence. Thus if
    route_id is given, the entire clause can only match a single leg.

    Note that the semantics of contains_id imply that a contains clause is defined across multiple
    rows of fare_rules.txt.

    OTP also supports a KCM extension where trips.txt supplies an extra column fare_id which
    associates a trip_id with a fare. The semantics of trip_id are the same as the semantics of
    route_id.
     */

    // Default fares

    // If a fare id is defined in fare_attributes.txt but is not mentioned by any rule in
    // fare_rules.txt, then the fare_id matches all single legs by default
    public void testDefaultFares() {
        FareRuleSet fareRuleSet = fareRuleSet();
        Ride ride = new Ride();
        assertFareMatch(fareRuleSet, ride);
        assertFareMatch(fareRuleSet, ride, ride);
    }

    // Routes

    // Test route match
    // Legs: (0) Route R1; (1) Route R2
    // fare_rules.txt:
    //   fare_id,route_id,origin_id,destination_id
    //   1,R1,,
    // Expected: (0) matches, (1) does not match, (0,1) does not match.
    public void testRouteMatch() {
        FareRuleSet rule = fareRuleSet(routeFareRule("R1"));
        Ride r0 = new Ride();
        r0.route = new AgencyAndId("1", "R1");
        Ride r1 = new Ride();
        r1.route = new AgencyAndId("1", "R2");
        assertFareMatch(rule, r0);
        assertNoFareMatch(rule, r1);
        assertNoFareMatch(rule, r0, r1);
    }

    // Test route match with multiple routes
    // Legs: (0) Route R1; (1) Route R2
    // fare_rules.txt:
    //   fare_id,route_id,origin_id,destination_id
    //   1,R1,,
    //   1,R2,,
    // Expected: (0) matches, (1) matches, (0,1) matches
    public void testMultipleRouteMatch() {
        FareRuleSet rule = fareRuleSet(routeFareRule("R1"), routeFareRule("R2"));
        Ride r0 = new Ride();
        r0.route = new AgencyAndId("1", "R1");
        Ride r1 = new Ride();
        r1.route = new AgencyAndId("1", "R2");
        assertFareMatch(rule, r0);
        assertFareMatch(rule, r1);
        assertFareMatch(rule, r0, r1);
    }

    // Origin/destination

    // Test origin destination with just origin or just destination
    // Legs: (0) from zone O to zone Z, (1) from zone Z to zone D. (2) from zone Z to zone Z
    // fare_rules.txt:
    //    fare_id,route_id,origin_id,destination_id
    //    1,,O,
    //    1,,,D
    // Expected: (0) matches, (1) matches, (2) does not match, (0, 2) matches, (2,0) does not match,
    //  (1,2) does not not match, (2,1) matches.
    public void testOriginDestinationMatch1() {
        FareRuleSet rule = fareRuleSet(odFareRule("O", null), odFareRule(null, "D"));
        Ride r0 = new Ride();
        r0.startZone = "O";
        r0.endZone = "Z";
        Ride r1 = new Ride();
        r1.startZone = "Z";
        r1.endZone = "D";
        Ride r2 = new Ride();
        r2.startZone = "Z";
        r2.endZone = "Z";
        assertFareMatch(rule, r0);
        assertFareMatch(rule, r1);
        assertNoFareMatch(rule, r2);
        assertFareMatch(rule, r0, r2);
    }

    // Test origin destination
    // Legs: (0) from zone O to zone D, (1) zone O to zone Z
    // fare_rules.txt:
    //    fare_id,route_id,origin_id,destination_id
    //    1,,,O,D
    // Expected: (0) matches, (1) does not match, (0, 1) does not match, (1, 0) matches
    public void testOriginDestinationMatch2() {
        FareRuleSet rule = fareRuleSet(odFareRule("O", "D"));
        Ride r0 = new Ride();
        r0.startZone = "O";
        r0.endZone = "D";
        Ride r1 = new Ride();
        r1.startZone = "O";
        r1.endZone = "Z";
        assertFareMatch(rule, r0);
        assertNoFareMatch(rule, r1);
        assertNoFareMatch(rule, r0, r1);
        assertFareMatch(rule, r1, r0);
    }

    // origin/destination across multiple rides
    // Legs: (0) from zone A to zone B, (1) zone B to zone C, (2) zone C to zone D
    // fare_rules.txt:
    //    fare_id,route_id,origin_id,destination_id
    //    1,,,A,D
    // Expected: Only (0,1,2) and (0,2) match
    public void testOriginDestinationMatch3() {
        FareRuleSet rule = fareRuleSet(odFareRule("A", "D"));
        Ride r0 = new Ride();
        r0.startZone = "A";
        r0.endZone = "B";
        Ride r1 = new Ride();
        r1.startZone = "B";
        r1.endZone = "C";
        Ride r2 = new Ride();
        r2.startZone = "C";
        r2.endZone = "D";
        assertNoFareMatch(rule, r0);
        assertNoFareMatch(rule, r1);
        assertNoFareMatch(rule, r2);
        assertFareMatch(rule, r0, r1, r2);
        assertFareMatch(rule, r0, r2);
        assertNoFareMatch(rule, r1, r2);
    }

    // match with contains

    // Legs: (0) thru zones (1,2), (1) thru zone 3, (2) thru zones (1,2,3,4)
    // fare_rules.txt:
    //    fare_id,contains_id
    //    1,1
    //    1,2
    //    1,3
    // Expected: (0,1) passes, (2) passes
    public void testContainsMatch() {
        FareRuleSet rule = fareRuleSet(containsFareRule("1"), containsFareRule("2"), containsFareRule("3"));
        Ride r0 = new Ride();
        r0.zones = Sets.newHashSet("1", "2");
        Ride r1 = new Ride();
        r1.zones = Sets.newHashSet("3");
        Ride r2 = new Ride();
        r2.zones = Sets.newHashSet("1", "2", "3", "4");
        assertNoFareMatch(rule, r0);
        assertNoFareMatch(rule, r1);
        assertFareMatch(rule, r2);
        assertFareMatch(rule, r0, r1);
    }

    // match with agency
    public void testAgencyMatch() {
        FareRuleSet rule = fareRuleSet();
        rule.setAgency("1");
        Ride r0 = new Ride();
        r0.agency = "1";
        Ride r1 = new Ride();
        r1.agency = "2";
        assertFareMatch(rule, r0);
        assertNoFareMatch(rule, r1);
        assertNoFareMatch(rule, r0, r1);
    }

    // match with multiple forms of matching - OR clauses
    // Legs: (0) from zone O to zone D on route B, (1) from zone Z to zone Z on route A
    // fare_rules.txt:
    //    fare_id,route_id,origin_id,destination_id
    //    1,A,,
    //    1,,O,D
    // Expected: (0) matches, (1) matches, (0,1) matches, (1,0) matches
    public void testMultipleMatches1() {
        FareRuleSet rule = fareRuleSet(routeFareRule("A"), odFareRule("O", "D"));
        Ride r0 = new Ride();
        r0.startZone = "O";
        r0.endZone = "D";
        r0.route = new AgencyAndId("1", "B");
        Ride r1 = new Ride();
        r1.route = new AgencyAndId("1", "A");
        r1.startZone = "Z";
        r1.endZone = "Z";
        assertFareMatch(rule, r0);
        assertFareMatch(rule, r1);
        assertFareMatch(rule, r0, r1);
        assertFareMatch(rule, r1, r0);
        assertNoFareMatch(rule, r0, r1, new Ride());
    }


    // Test that O/D match happens across entire itinerary.
    // itinerary: (0) Route B from zone O to zone O; (1) Route A from zone O to zone O, (2) Route C from Zone O to zone D
    // fare_rules.txt:
    //   fare_id,route_id,origin_id,destination_id
    //   1,,O,D
    //   1,A,,
    // Expected: Match: (1), (1,2) matches, (0,2) matches, (0,1,2) matches
    // No match: (0,1)
    public void testMultipleMatches2() {
        FareRuleSet rule = fareRuleSet(routeFareRule("A"), odFareRule("O", "D"));
        Ride r0 = new Ride();
        r0.startZone = "O";
        r0.endZone = "O";
        r0.route = new AgencyAndId("1", "B");
        Ride r1 = new Ride();
        r1.startZone = "O";
        r1.endZone = "O";
        r1.route = new AgencyAndId("1", "A");
        Ride r2 = new Ride();
        r2.startZone = "O";
        r2.endZone = "D";
        r2.route = new AgencyAndId("1", "C");
        assertFareMatch(rule, r1);
        assertFareMatch(rule, r1, r2);
        assertFareMatch(rule, r0, r2);
        assertFareMatch(rule, r0, r1, r2);
        assertNoFareMatch(rule, r0, r1);
    }

    // match with multiple forms of matching - OR clauses with contains
    // Legs: (0) route B thru zone (1,2), (1) route A thru zone 2, (2) route B thru zone 2
    // fare_rules.txt:
    //    fare_id,route_id,contains_id
    //    1,A,,
    //    1,,1
    // Expected: (0) matches, (1) matches, (0,1) matches, (2) does not match
    public void testMultipleMatches3() {
        FareRuleSet rule = fareRuleSet(routeFareRule("A"), containsFareRule("1"), containsFareRule("2"));
        Ride r0 = new Ride();
        r0.zones = Sets.newHashSet("1", "2", "3");
        r0.route = new AgencyAndId("1", "B");
        Ride r1 = new Ride();
        r1.route = new AgencyAndId("1", "A");
        r1.zones = Sets.newHashSet("2");
        Ride r2 = new Ride();
        r2.route = new AgencyAndId("1", "B");
        r2.zones = Sets.newHashSet("2");
        assertFareMatch(rule, r0);
        assertFareMatch(rule, r1);
        assertFareMatch(rule, r0, r1);
        assertFareMatch(rule, r1, r0);
        assertFareMatch(rule, r0, r1, r2);
        assertNoFareMatch(rule, r2);
        assertNoFareMatch(rule, r1, r2);
    }

    // Test that a rule with a route clause must only apply to a leg.
    // itinerary: (0) Route A from zone O to zone Z; (1) Route B from zone Z to zone D.
    // (2) Route A from zone O to zone D, (3) Route B from zone O to zone D.
    // fare_rules.txt:
    //   fare_id,route_id,origin_id,destination_id
    //   1,A,O,D
    //   1,B,O,D
    // Expected: Match: (2), (3), (2,3). No match: (0), (1), (0,1), (0,1,2).
    public void testRoutesAndODsAcrossMultiple() {
        FareRuleSet rule = fareRuleSet(fareRule("A", "O", "D", null), fareRule("B", "O", "D", null));
        Ride r0 = new Ride();
        r0.route = new AgencyAndId("1", "A");
        r0.startZone = "O";
        r0.endZone = "Z";
        Ride r1 = new Ride();
        r1.route = new AgencyAndId("1", "B");
        r1.startZone = "Z";
        r1.endZone = "D";
        Ride r2 = new Ride();
        r2.route = new AgencyAndId("1", "A");
        r2.startZone = "O";
        r2.endZone = "D";
        Ride r3 = new Ride();
        r3.route = new AgencyAndId("1", "B");
        r3.startZone = "O";
        r3.endZone = "D";
        assertNoFareMatch(rule, r0);
        assertNoFareMatch(rule, r1);
        assertNoFareMatch(rule, r0, r1);
        assertFareMatch(rule, r2);
        assertFareMatch(rule, r3);
        assertFareMatch(rule, r2, r3);
        assertNoFareMatch(rule, r0, r1, r2);
    }

    // Test that a rule with a route clause must only apply to a leg - check with contains
    // itinerary: (0) Route A across zones (1,2)
    //            (1) Route B across zones (1,2)
    //            (2) Route A across (1)
    // fare_rules.txt:
    //   fare_id,route_id,contains_id
    //   1,A,1
    //   1,A,2
    // Expected: Match: (0). No match: (1), (2)
    public void testRoutesAndContains() {
        FareRuleSet rule = fareRuleSet(fareRule("A", null, null, "1"),
                fareRule("A", null, null, "2"));
        Ride r0 = new Ride();
        r0.route = new AgencyAndId("1", "A");
        r0.zones = Sets.newHashSet("1", "2");
        Ride r1 = new Ride();
        r1.route = new AgencyAndId("1", "B");
        r1.zones = Sets.newHashSet("1", "2");
        Ride r2 = new Ride();
        r2.route = new AgencyAndId("1", "A");
        r2.zones = Sets.newHashSet("1");
        assertFareMatch(rule, r0);
        assertNoFareMatch(rule, r1);
        assertNoFareMatch(rule, r2);
        assertNoFareMatch(rule, r1, r2);
    }

    // whole set properties

    public void testNumTransfers() {
        Ride r0 = new Ride();
        Ride r1 = new Ride();
        Ride r2 = new Ride();

        FareRuleSet rule = fareRuleSet();
        assertFareMatch(rule, r0, r1, r2);

        rule.getFareAttribute().setTransfers(0);
        assertNoFareMatch(rule, r0, r1, r2);
        assertNoFareMatch(rule, r0, r1);

        rule.getFareAttribute().setTransfers(1);
        assertNoFareMatch(rule, r0, r1, r2);
        assertFareMatch(rule, r0, r1);

        rule.getFareAttribute().setTransfers(2);
        assertFareMatch(rule, r0, r1, r2);
    }

    public void testTransferDuration() {
        Ride r0 = new Ride();
        r0.startTime = 0;
        r0.endTime = 10;
        Ride r1 = new Ride();
        r1.startTime = 15;
        r1.endTime = 20;
        Ride r2 = new Ride();
        r2.startTime = 20;
        r2.endTime = 30;

        FareRuleSet rule = fareRuleSet();
        rule.getFareAttribute().setTransferDuration(15);

        assertFareMatch(rule, r0, r1);
        assertNoFareMatch(rule, r0, r2);
    }


    public void testJourneyDuration() {
        Ride r0 = new Ride();
        r0.startTime = 0;
        r0.endTime = 10;
        Ride r1 = new Ride();
        r1.startTime = 15;
        r1.endTime = 20;
        Ride r2 = new Ride();
        r2.startTime = 20;
        r2.endTime = 30;

        FareRuleSet rule = fareRuleSet();
        rule.getFareAttribute().setJourneyDuration(25);

        assertFareMatch(rule, r0, r1);
        assertNoFareMatch(rule, r0, r2);
    }

    private FareRuleSet fareRuleSet(FareRule... rules) {
        FareAttribute attr = new FareAttribute();
        attr.setId(new AgencyAndId("1","F1"));
        FareRuleSet fareRuleSet = new FareRuleSet(attr);
        for (FareRule rule : rules) {
            fareRuleSet.addFareRule(rule);
        }
        return fareRuleSet;
    }

    private FareRule containsFareRule(String contains) {
        return fareRule(null, null, null, contains);
    }

    private FareRule odFareRule(String origin, String destination) {
        return fareRule(null, origin, destination, null);
    }

    private FareRule routeFareRule(String routeId) {
        return fareRule(routeId, null, null, null);
    }

    private FareRule fareRule(String routeId, String origin, String destination, String contains) {
        FareRule fareRule = new FareRule();
        if (routeId != null) {
            Route route = new Route();
            route.setId(new AgencyAndId("1", routeId));
            fareRule.setRoute(route);
        }
        if (origin != null) {
            fareRule.setOriginId(origin);
        }
        if (destination != null) {
            fareRule.setDestinationId(destination);
        }
        if (contains != null) {
            fareRule.setContainsId(contains);
        }
        return fareRule;
    }

    private void assertFareMatch(FareRuleSet ruleSet, Ride... rides) {
        assertTrue(fareMatch(ruleSet, rides));
    }

    private void assertNoFareMatch(FareRuleSet ruleSet, Ride... rides) {
        assertFalse(fareMatch(ruleSet, rides));
    }

    private boolean fareMatch(FareRuleSet ruleSet, Ride... rides) {
        return ruleSet.matches(new RideSequence(Arrays.asList(rides)));
    }
}
