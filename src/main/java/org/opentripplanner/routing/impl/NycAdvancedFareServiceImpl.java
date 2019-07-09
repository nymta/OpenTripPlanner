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

import com.sun.jdi.connect.spi.TransportService;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareBundle;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.edgetype.DwellEdge;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/** NYC fare condition types */
enum NycFareConditionType {
    peak_hour_only,
    non_peak_hour_only
}

/** NYC transfer agreement types */
enum NycTransferType {
    free,
    free_step_up
}

/** NYC peak hour rule types */
enum NycPeakHourRuleType {
    departure,
    arrival
}

/** A service is a combination of agency and route type */
class NycServiceId implements Serializable {
    String agencyId;
    int routeType;

    NycServiceId(String agencyId, int routeType) {
        this.agencyId = agencyId;
        this.routeType = routeType;
    }

    public String getAgencyId() {
        return this.agencyId;
    }
    public int getRouteType() {
        return this.routeType;
    }

    public String toString() {
        return convertToString(this);
    }

    public static String convertToString(NycServiceId sid) {
        return sid == null ? null : sid.getAgencyId() + '_' + sid.getRouteType();
    }
}

/** Holds service basic fare info */
class NycAgencyFare implements Serializable {
    NycServiceId serviceId;
    FareType fareType;
    NycFareConditionType fareConditionType;
    float price;
    String startZone;
    String endZone;

    NycAgencyFare(NycServiceId serviceId,
                  FareType fareType,
                  NycFareConditionType fareConditionType,
                  float price,
                  String startZone,
                  String endZone) {
        this.serviceId = serviceId;
        this.fareType = fareType;
        this.fareConditionType = fareConditionType;
        this.price = price;
        this.startZone = startZone;
        this.endZone = endZone;
    }

    public String getKey() {
        String internalKey = this.serviceId.toString() + '_' + this.fareType.toString();
        if(this.fareConditionType != null) {
            internalKey += '_' + this.fareConditionType.toString();
        }
        if(this.startZone != null && !this.startZone.isEmpty()) {
            internalKey += '_' + this.startZone;
        }
        if(this.endZone != null && !this.endZone.isEmpty()) {
            internalKey += '_' + this.endZone;
        }

        return internalKey;
    }
}

/** Holds agency peak hours */
class NycAgencyPeakHour implements Serializable {
    NycServiceId serviceId;
    NycPeakHourRuleType peakHourRuleType; //departure, arrival, or both
    String stopId; //peak hour only applied to certain stops if configured
    int[] days; // most likely weekday
    int[] hours;

    NycAgencyPeakHour(NycServiceId serviceId, NycPeakHourRuleType peakHourRuleType, String stopId, int[] days, int[] hours) {
        this.serviceId = serviceId;
        this.peakHourRuleType = peakHourRuleType;
        this.stopId = stopId;
        this.days = days;
        this.hours = hours;
    }

    public String getKey() {
        String internalKey = this.serviceId.toString();
        if(this.stopId != null && !this.stopId.isEmpty()) {
            internalKey += '_' + this.stopId;
        }
        if(this.peakHourRuleType != null) {
            internalKey += '_' + this.peakHourRuleType.toString();
        }
        return internalKey;
    }
}

/** Holds transfer agreement rules between a pair of services */
class NycTransferRule implements Serializable {
    NycServiceId serviceId;
    NycServiceId otherServiceId;
    NycTransferType transferType;
    int transferDuration;

    NycTransferRule(NycServiceId serviceId, NycServiceId otherServiceId, NycTransferType transferType, int transferDuration) {
        this.serviceId = serviceId;
        this.otherServiceId = otherServiceId;
        this.transferType = transferType;
        this.transferDuration = transferDuration;
    }

    public String getKey() {
        return this.serviceId.toString() + '_' + this.otherServiceId.toString();
    }
}

/** Internal class to hold previous traveled services */
class NycTraveledService implements Serializable {
    NycServiceId serviceId;
    AgencyAndId agencyAndId;
    long rideTime;
    float price;

    NycTraveledService(NycServiceId serviceId, AgencyAndId agencyAndId, long rideTime, float price) {
        this.serviceId = serviceId;
        this.agencyAndId = agencyAndId;
        this.rideTime = rideTime;
        this.price = price;
    }

    public String getKey() {
        return this.agencyAndId.toString() + '_' + this.rideTime;
    }
}

/**
 * This handles the New York City advanced fare rules:
 * 1. fixed fare
 * 2. zone-to-zone fare
 * 3. service-to-service transfer rules
 * 4. peak hour rules
 */
public class NycAdvancedFareServiceImpl implements FareService, Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(NycFareServiceImpl.class);

    private static final long serialVersionUID = 1L;
    HashMap<String, NycAgencyFare> agencyFares = new HashMap<String, NycAgencyFare>();
    HashMap<String, NycTransferRule> transferRules = new HashMap<String, NycTransferRule>();
    HashMap<String, NycAgencyPeakHour> agencyPeakHours = new HashMap<String, NycAgencyPeakHour>();

    public NycAdvancedFareServiceImpl() {
        //add some testing data
        NycServiceId nyctSubway = new NycServiceId("MTASBWY", 1);
        NycServiceId nyctLocalBus = new NycServiceId("MTA NYCT", 3);
        NycServiceId nyctExpressBus = new NycServiceId("MTA NYCT", 702);
        NycServiceId mtabcLocalBus = new NycServiceId("MTABC", 3);
        NycServiceId mtabcExpressBus = new NycServiceId("MTABC", 702);

        // agency fares
        NycAgencyFare nyctSubwayRegularFare = new NycAgencyFare(nyctSubway, FareType.regular, null, 2.75f, null, null);
        NycAgencyFare nyctSubwayReducedFare = new NycAgencyFare(nyctSubway, FareType.special, null, 1.35f, null, null);
        NycAgencyFare nyctLocalBusRegularFare = new NycAgencyFare(nyctLocalBus, FareType.regular, null, 2.75f, null, null);
        NycAgencyFare nyctLocalBusReducedFare = new NycAgencyFare(nyctLocalBus, FareType.special, null, 1.35f, null, null);
        NycAgencyFare nyctExpressBusRegularFare = new NycAgencyFare(nyctExpressBus, FareType.regular, null, 6.75f, null, null);
        NycAgencyFare nyctExpressBusReducedFare = new NycAgencyFare(nyctExpressBus, FareType.special, NycFareConditionType.peak_hour_only, 3.35f, null, null);
        NycAgencyFare mtabcLocalBusRegularFare = new NycAgencyFare(mtabcLocalBus, FareType.regular, null, 2.75f, null, null);
        NycAgencyFare mtabcLocalBusReducedFare = new NycAgencyFare(mtabcLocalBus, FareType.special, null, 1.35f, null, null);
        NycAgencyFare mtabcExpressBusRegularFare = new NycAgencyFare(mtabcExpressBus, FareType.regular, null, 6.75f, null, null);
        NycAgencyFare mtabcExpressBusReducedFare = new NycAgencyFare(mtabcExpressBus, FareType.special, NycFareConditionType.peak_hour_only, 3.35f, null, null);
        agencyFares.put(nyctSubwayRegularFare.getKey(), nyctSubwayRegularFare);
        agencyFares.put(nyctSubwayReducedFare.getKey(), nyctSubwayReducedFare);
        agencyFares.put(nyctLocalBusRegularFare.getKey(), nyctLocalBusRegularFare);
        agencyFares.put(nyctLocalBusReducedFare.getKey(), nyctLocalBusReducedFare);
        agencyFares.put(nyctExpressBusRegularFare.getKey(), nyctExpressBusRegularFare);
        agencyFares.put(nyctExpressBusReducedFare.getKey(), nyctExpressBusReducedFare);
        agencyFares.put(mtabcLocalBusRegularFare.getKey(), mtabcLocalBusRegularFare);
        agencyFares.put(mtabcLocalBusReducedFare.getKey(), mtabcLocalBusReducedFare);
        agencyFares.put(mtabcExpressBusRegularFare.getKey(), mtabcExpressBusRegularFare);
        agencyFares.put(mtabcExpressBusReducedFare.getKey(), mtabcExpressBusReducedFare);

        // transfer rules
        // same service
        NycTransferRule nyctSubwayToSubway = new NycTransferRule(nyctSubway, nyctSubway, NycTransferType.free, 120 * 60);
        NycTransferRule nyctLocalToLocal = new NycTransferRule(nyctLocalBus, nyctLocalBus, NycTransferType.free, 120 * 60);
        NycTransferRule mtabcLocalToLocal = new NycTransferRule(mtabcLocalBus, mtabcLocalBus, NycTransferType.free, 120 * 60);

        // local bus
        NycTransferRule nyctSubwayToLocalBus = new NycTransferRule(nyctSubway, nyctLocalBus, NycTransferType.free, 120 * 60);
        NycTransferRule nyctSubwayToMtabcLocal = new NycTransferRule(nyctSubway, mtabcLocalBus, NycTransferType.free, 120 * 60);
        NycTransferRule nyctLocalToMtabcLocal = new NycTransferRule(nyctLocalBus, mtabcLocalBus, NycTransferType.free, 120 * 60);

        // express bus
        NycTransferRule nyctSubwayToExpressBus = new NycTransferRule(nyctSubway, nyctExpressBus, NycTransferType.free_step_up, 120 * 60);
        NycTransferRule nyctSubwayToMtabcExpress = new NycTransferRule(nyctSubway, mtabcExpressBus, NycTransferType.free_step_up, 120 * 60);
        NycTransferRule nyctLocalToExpressBus = new NycTransferRule(nyctLocalBus, nyctExpressBus, NycTransferType.free_step_up, 120 * 60);
        NycTransferRule nyctLocalToMtabcExpress = new NycTransferRule(nyctLocalBus, mtabcExpressBus, NycTransferType.free_step_up, 120 * 60);
        NycTransferRule mtabcLocalToExpressBus = new NycTransferRule(mtabcLocalBus, mtabcExpressBus, NycTransferType.free_step_up, 120 * 60);
        NycTransferRule mtabcLocalToNyctExpress = new NycTransferRule(mtabcLocalBus, nyctExpressBus, NycTransferType.free_step_up, 120 * 60);

        transferRules.put(nyctSubwayToSubway.getKey(), nyctSubwayToSubway);
        transferRules.put(nyctSubwayToLocalBus.getKey(), nyctSubwayToLocalBus);
        transferRules.put(nyctSubwayToExpressBus.getKey(), nyctSubwayToExpressBus);
        transferRules.put(nyctLocalToExpressBus.getKey(), nyctLocalToExpressBus);
        transferRules.put(nyctLocalToLocal.getKey(), nyctLocalToLocal);
        transferRules.put(mtabcLocalToLocal.getKey(), mtabcLocalToLocal);
        transferRules.put(nyctSubwayToMtabcLocal.getKey(), nyctSubwayToMtabcLocal);
        transferRules.put(nyctLocalToMtabcLocal.getKey(), nyctLocalToMtabcLocal);
        transferRules.put(nyctSubwayToMtabcExpress.getKey(), nyctSubwayToMtabcExpress);
        transferRules.put(nyctLocalToMtabcExpress.getKey(), nyctLocalToMtabcExpress);
        transferRules.put(mtabcLocalToExpressBus.getKey(), mtabcLocalToExpressBus);
        transferRules.put(mtabcLocalToNyctExpress.getKey(), mtabcLocalToNyctExpress);

        // peak hours
        int[] weekdays = new int[]{1,2,3,4,5};
        int[] hours = new int[] {6,7,8,9,10,15,16,17,18,19};
        NycAgencyPeakHour nyctPeakHours = new NycAgencyPeakHour(nyctExpressBus, null, null, weekdays, hours);
        NycAgencyPeakHour mtabcPeakHours = new NycAgencyPeakHour(mtabcExpressBus, null, null, weekdays, hours);
        agencyPeakHours.put(nyctPeakHours.getKey(), nyctPeakHours);
        agencyPeakHours.put(mtabcPeakHours.getKey(), mtabcPeakHours);
    }

    @Override
    public Fare getCost(GraphPath path) {
        FareBundle fareBundle = getLegCostBreakDown(path);

        if(fareBundle != null) {
            return fareBundle.fare;
        } else {
            return null;
        }
    }

    @Override
    public FareBundle getLegCostBreakDown(GraphPath path) {
        LinkedList<State> states = path.states;
        final int WALK = -1;
        final int SUBWAY = 1;

        // create rides
        List<Ride> rides = new ArrayList<Ride>();
        Ride newRide = null;

        for (State state : states) {
            Edge backEdge = state.getBackEdge();
            if (backEdge instanceof StreetEdge) {
                if (newRide == null || !newRide.classifier.equals(WALK)) {
                    if (rides.size() == 0 || !rides.get(rides.size() - 1).classifier.equals(WALK)) {
                        newRide = new Ride();
                        newRide.classifier = WALK;
                        rides.add(newRide);
                    }
                }
                continue;
            }

            // dwells do not affect fare.
            if (backEdge instanceof DwellEdge)
                continue;

            if (!(backEdge instanceof HopEdge)) {
                newRide = null;
                continue;
            }
            AgencyAndId routeId = state.getRoute();
            Agency agency = state.getBackTrip().getRoute().getAgency();
            String agencyId = agency.getId();
            if (routeId == null) {
                newRide = null;
            } else {
                if (newRide == null || !routeId.equals(newRide.route)) {
                    newRide = new Ride();
                    newRide.agency = agency.getId();
                    rides.add(newRide);

                    newRide.firstStop = ((HopEdge) backEdge).getBeginStop();

                    newRide.route = routeId;
                    Trip trip = state.getBackTrip();
                    Route route = trip.getRoute();
                    int type = route.getType();
                    newRide.routeType = type;
                    NycServiceId serviceId = new NycServiceId(agencyId, type);
                    newRide.classifier = serviceId;
                    newRide.startTime = state.getTimeSeconds();
                }
                newRide.lastStop = ((HopEdge) backEdge).getBeginStop();
            }
        }

        // There are no rides, so there's no fare.
        if (rides.size() == 0) {
            return null;
        }

        Currency currency = Currency.getInstance("USD");
        Fare totalFare = new Fare();
        Map<String, Fare> legFares = new HashMap<String, Fare>();
        for (FareType fareType : FareType.values()) {
            // Holds previously traveled services in the same ride
            Set<NycTraveledService> traveledServices = new HashSet<NycTraveledService>();

            float totalPrice = 0.0f;
            HashSet<AgencyAndId> unknownFareRoutes = new HashSet<AgencyAndId>(); //TODO: how to include it into the response
            int prevRouteType = WALK;


            //iterate rides to calculate total fare
            boolean hasFare = false; // whether fare exists
            for (Ride ride : rides) {
                // walk is free
                if (ride.classifier.equals(WALK)) {
                    prevRouteType = WALK;
                    continue;
                }

                // if in-station subway transfer, then free
                if (ride.routeType == prevRouteType && ride.routeType == SUBWAY) {
                    prevRouteType = ride.routeType;
                    continue;
                }

                long currentRideTime = ride.startTime;

                // find agency fare settings
                NycAgencyFare agencyFare = findAgencyFare(ride, fareType);

                // log it if fare unknown
                if (agencyFare == null) {
                    unknownFareRoutes.add(ride.route);
                    prevRouteType = ride.routeType;
                    continue;
                }

                hasFare = true;

                // get basic fare without transfer
                float basicFare = agencyFare.price;

                // start calculating transfer fare
                boolean transferFound = false;
                float transferFare = basicFare;

                NycTraveledService targetPrevTraveledService = null; // the target that applies transfer rule into current ride
                for (NycTraveledService traveledService : traveledServices) {
                    String prevServiceIdString = traveledService.serviceId.toString();

                    Set<NycTransferRule> rules = getRelatedTransferRules(prevServiceIdString);
                    if (rules.isEmpty()) {
                        continue;
                    }

                    // calculate transfer fare
                    long prevServiceRideTime = traveledService.rideTime;
                    for (NycTransferRule tRule : rules) {
                        if (tRule.transferDuration > 0) {
                            // first check transfer duration
                            if ((currentRideTime - prevServiceRideTime > tRule.transferDuration)) {
                                continue;
                            }
                        }

                        // loop transfer type
                        switch (tRule.transferType) {
                            case free:
                                transferFound = true;
                                transferFare = 0.0f;
                                break;
                            case free_step_up:
                                transferFound = true;
                                if (traveledService.price >= transferFare) {
                                    transferFare = 0.0f;
                                } else {
                                    transferFare -= traveledService.price;
                                }
                                break;
                            default:
                                break;

                        }

                        // transfer found
                        if (transferFound) {
                            break;
                        }
                    }

                    if (transferFound) {
                        targetPrevTraveledService = traveledService;
                        break;
                    }
                }

                float legPrice = 0;
                if (!transferFound) {
                    legPrice = basicFare;
                    // no transfer rules from previous services, add basic fare to total
                    totalPrice += legPrice;
                    if (legPrice > 0) {
                        // log paid service so future rides can check transfer rules
                        NycTraveledService traveledService = new NycTraveledService(new NycServiceId(ride.agency, ride.routeType), ride.route, currentRideTime, basicFare);
                        traveledServices.add(traveledService);
                    }
                } else {
                    legPrice = transferFare;
                    // transfer rule found, add transfer fare to total
                    totalPrice += legPrice;

                    // One transfer is allowed in one trip
                    // remove the service that covers the transfer to current ride
                    if (targetPrevTraveledService != null) {
                        traveledServices.remove(targetPrevTraveledService);
                    }
                }

                String routeId = ride.route.toString();
                if(!legFares.containsKey(routeId)) {
                    legFares.put(routeId, new Fare());
                }

                Fare legFare = legFares.get(routeId);
                legFare.addFare(fareType, new WrappedCurrency(currency),
                        (int) Math.round(legPrice
                                * Math.pow(10, currency.getDefaultFractionDigits())));

                prevRouteType = ride.routeType;
            }

            if(hasFare) {
                totalFare.addFare(fareType, new WrappedCurrency(currency),
                        (int) Math.round(totalPrice
                                * Math.pow(10, currency.getDefaultFractionDigits())));
            }
        }

        return new FareBundle(totalFare, legFares);
    }


    /** find agency fare for the ride based on requested fare type */
    private NycAgencyFare findAgencyFare(Ride ride, FareType fareType) {
        String serviceIdString = ride.agency + "_" + ride.routeType;
        boolean isPeak = isInPeakHour(ride);
        // zone-pair key component (used in finding specific fare rule)
        String zoneKey = "";
        if(ride.startZone != null && !ride.startZone.isEmpty()) {
            zoneKey += '_' + ride.startZone;
        }
        if(ride.endZone != null && !ride.endZone.isEmpty()) {
            zoneKey += '_' + ride.endZone;
        }

        // check if there's fare without conditions
        String nonConditionFareKey = serviceIdString + '_' + fareType.toString();
        if(!zoneKey.isEmpty()) {
            nonConditionFareKey += zoneKey;
        }
        NycAgencyFare nonConditionFare = agencyFares.get(nonConditionFareKey);

        // check if there's fare with conditions
        NycAgencyFare conditionFare = null;
        for (NycFareConditionType fareConditionType : NycFareConditionType.values()) {
            String fareKey = serviceIdString + '_' + fareType.toString() + "_" + fareConditionType.toString();
            // first check fixed fare (without startZone & endZone)
            conditionFare = agencyFares.get(fareKey);
            if(conditionFare == null) {
                fareKey += zoneKey;
                conditionFare = agencyFares.get(fareKey);
            }

            if(conditionFare == null) {
                continue;
            }

            // check if meet fare conditions
            switch (conditionFare.fareConditionType) {
                case peak_hour_only :
                    if(!isPeak) {
                        conditionFare = null; // reset if not meet
                    }
                case non_peak_hour_only :
                    if(isPeak) {
                        conditionFare = null; // reset if not meet
                    }

                    break;
                default:
                    break;
            }

            if(conditionFare != null) {
                break;
            }
        }

        if(conditionFare != null) {
            return conditionFare;
        } else {
            return nonConditionFare;
        }
    }

    /** check if the ride is in Peak Hour */
    private boolean isInPeakHour(Ride ride) {
        String ruleKey = ride.agency + "_" + ride.routeType;
        NycAgencyPeakHour peakHours = agencyPeakHours.get(ruleKey);
        // get peak hour rules
        if(peakHours == null) {
            // check specific stop, first with startZone
            if(ride.startZone != null && !ride.startZone.isEmpty()) {
                String startZoneRuleKey = ruleKey + "_" + ride.startZone;
                peakHours = agencyPeakHours.get(startZoneRuleKey);
                if(peakHours == null) {
                    // check rule type
                    startZoneRuleKey += startZoneRuleKey + '_' + NycPeakHourRuleType.departure;
                    peakHours = agencyPeakHours.get(startZoneRuleKey);
                }
            }

            // check end zone
            if(peakHours == null && ride.endZone != null && !ride.endZone.isEmpty()) {
                String endZoneRuleKey = ruleKey + "_" + ride.endZone;
                peakHours = agencyPeakHours.get(endZoneRuleKey);
                if (peakHours == null) {
                    // check rule type
                    endZoneRuleKey = endZoneRuleKey + '_' + NycPeakHourRuleType.arrival;
                    peakHours = agencyPeakHours.get(endZoneRuleKey);
                }
            }
        }

        // no peak hour rules for this service
        if(peakHours == null) {
            return false;
        }

        // check ride time against peak hour days and hours
        long rideTimeSeconds = ride.startTime;
        if(peakHours.peakHourRuleType == NycPeakHourRuleType.arrival) {
            rideTimeSeconds = ride.endTime;
        }

        Date rideTime = new Date(rideTimeSeconds * 1000);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(rideTime);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        return Arrays.asList(peakHours.days).contains(dayOfWeek) && Arrays.asList(peakHours.hours).contains(hour);
    }

    /** query transfer rules related to a specific service */
    private Set<NycTransferRule> getRelatedTransferRules(String serviceIdString) {
        return transferRules.entrySet().stream()
                .filter(
                        r -> (
                                r.getValue().serviceId.toString().equals(serviceIdString) ||
                                        r.getValue().otherServiceId.toString().equals(serviceIdString)
                        )
                )
                .map(p -> p.getValue())
                .collect(Collectors.toSet());
    }
}
