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
    free_step_up,
    merge //Used for Zones fares with transfers
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
    String midZone;

    NycAgencyFare(NycServiceId serviceId,
                  FareType fareType,
                  NycFareConditionType fareConditionType,
                  float price,
                  String startZone,
                  String endZone,
                  String midZone) {
        this.serviceId = serviceId;
        this.fareType = fareType;
        this.fareConditionType = fareConditionType;
        this.price = price;
        this.startZone = startZone;
        this.endZone = endZone;
        this.midZone = midZone;
    }

    public String getKey() {
        String internalKey = this.serviceId.toString() + '_' + this.fareType.toString();
        if(this.fareConditionType != null) {
            internalKey += '_' + this.fareConditionType.toString();
        }
        if(this.startZone != null && !this.startZone.isEmpty()) {
            internalKey += "_" + this.startZone;
        }
        if(this.midZone != null && !this.midZone.isEmpty()) {
            internalKey += "_" + this.midZone;
        }
        if(this.endZone != null && !this.endZone.isEmpty()) {
            internalKey += "_" + this.endZone;
        }


        return internalKey;
    }
}

/** Holds agency peak hours */
class NycAgencyPeakHour implements Serializable {
    NycServiceId serviceId;
    Integer[] days; // most likely weekday
    Integer[] hours;
    boolean useGtfs;

    NycAgencyPeakHour(NycServiceId serviceId, Integer[] days, Integer[] hours, boolean useGtfs) {
        this.serviceId = serviceId;
        this.days = days;
        this.hours = hours;
        this.useGtfs = useGtfs;
    }

    public String getKey() {
        return this.serviceId.toString();
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
    String startZone;
    String midZone;

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


        ////////////////////////////////////////////////////////
        // Create Agencies
        ///////////////////////////////////////////////////////

        NycServiceId nyctSubway = new NycServiceId("MTASBWY", 1);
        NycServiceId nyctLocalBus = new NycServiceId("MTA NYCT", 3);
        NycServiceId nyctExpressBus = new NycServiceId("MTA NYCT", 702);
        NycServiceId mtabcLocalBus = new NycServiceId("MTABC", 3);
        NycServiceId mtabcExpressBus = new NycServiceId("MTABC", 702);
        NycServiceId lirr = new NycServiceId("LI", 2);
        NycServiceId mnr = new NycServiceId("MNR", 2);

        ////////////////////////////////////////////////////////
        // Create Agency Fares
        ///////////////////////////////////////////////////////

        ///////////////////
        // Subway and Bus
        ///////////////////
        NycAgencyFare nyctSubwayRegularFare = new NycAgencyFare(nyctSubway, FareType.regular, null, 2.75f, null, null, null);
        NycAgencyFare nyctSubwayReducedFare = new NycAgencyFare(nyctSubway, FareType.special, null, 1.35f, null, null, null);
        NycAgencyFare nyctLocalBusRegularFare = new NycAgencyFare(nyctLocalBus, FareType.regular, null, 2.75f, null, null, null);
        NycAgencyFare nyctLocalBusReducedFare = new NycAgencyFare(nyctLocalBus, FareType.special, null, 1.35f, null, null, null);
        NycAgencyFare nyctExpressBusRegularFare = new NycAgencyFare(nyctExpressBus, FareType.regular, null, 6.75f, null, null, null);
        NycAgencyFare nyctExpressBusReducedFare = new NycAgencyFare(nyctExpressBus, FareType.special, NycFareConditionType.peak_hour_only, 3.35f, null, null, null);
        NycAgencyFare mtabcLocalBusRegularFare = new NycAgencyFare(mtabcLocalBus, FareType.regular, null, 2.75f, null, null, null);
        NycAgencyFare mtabcLocalBusReducedFare = new NycAgencyFare(mtabcLocalBus, FareType.special, null, 1.35f, null, null, null);
        NycAgencyFare mtabcExpressBusRegularFare = new NycAgencyFare(mtabcExpressBus, FareType.regular, null, 6.75f, null, null, null);
        NycAgencyFare mtabcExpressBusReducedFare = new NycAgencyFare(mtabcExpressBus, FareType.special, NycFareConditionType.peak_hour_only, 3.35f, null, null, null);

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

        ///////////////////
        // LIRR
        ///////////////////

        //////
        // LIRR Off Peak Direct Routes
        //////

        HashMap<String, Float> lirrFareMap = new HashMap<String, Float>();
        // Zone 1
        lirrFareMap.put("1to1", 6.5f);
        lirrFareMap.put("3to1", 7.75f);
        lirrFareMap.put("4to1", 9.25f);
        lirrFareMap.put("7to1", 10.25f);
        lirrFareMap.put("9to1", 12f);
        lirrFareMap.put("10to1", 14.25f);
        lirrFareMap.put("12to1", 17f);
        lirrFareMap.put("14to1", 22.25f);

        // Zone 3
        lirrFareMap.put("3to3", 4f);
        lirrFareMap.put("4to3", 6f);
        lirrFareMap.put("7to3", 7.25f);
        lirrFareMap.put("9to3", 9f);
        lirrFareMap.put("10to3", 11f);
        lirrFareMap.put("12to3", 14.75f);
        lirrFareMap.put("14to3", 19f);

        // Zone 4
        lirrFareMap.put("4to4", 3.25f);
        lirrFareMap.put("7to4", 3.25f);
        lirrFareMap.put("9to4", 6f);
        lirrFareMap.put("10to4", 7.5f);
        lirrFareMap.put("12to4", 11.25f);
        lirrFareMap.put("14to4", 17.75f);

        // Zone 7
        lirrFareMap.put("7to7", 3.25f);
        lirrFareMap.put("9to7", 3.25f);
        lirrFareMap.put("10to7", 6f);
        lirrFareMap.put("12to7", 10f);
        lirrFareMap.put("14to7", 16.5f);

        // Zone 9
        lirrFareMap.put("9to9", 3.25f);
        lirrFareMap.put("10to9", 3.25f);
        lirrFareMap.put("12to9", 7.5f);
        lirrFareMap.put("14to9", 13.5f);

        // Zone 10
        lirrFareMap.put("10to10", 3.25f);
        lirrFareMap.put("12to10", 3.25f);
        lirrFareMap.put("14to10", 9.5f);

        // Zone 12
        lirrFareMap.put("12to12", 3.25f);
        lirrFareMap.put("14to12", 6.5f);

        // Zone 14
        lirrFareMap.put("14to14", 3.25f);

        for (HashMap.Entry<String, Float> entry : lirrFareMap.entrySet()) {
            String key = entry.getKey();
            Float value = entry.getValue();
            String startZone = key.split("to")[0];
            String endZone = key.split("to")[1];
            NycAgencyFare lirrFare= new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.non_peak_hour_only, value.floatValue(), startZone, endZone,null);
            agencyFares.put(lirrFare.getKey(), lirrFare);
            if(!endZone.equals(startZone)) {
                NycAgencyFare olirrFare= new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.non_peak_hour_only, value.floatValue(), endZone, startZone,null);
                agencyFares.put(olirrFare.getKey(), olirrFare);
            }
        }

        //////
        // LIRR Peak Direct Routes
        //////
        lirrFareMap.clear();

        // Zone 1
        lirrFareMap.put("1to1", 9f);
        lirrFareMap.put("3to1", 10.75f);
        lirrFareMap.put("4to1", 12.5f);
        lirrFareMap.put("7to1", 14f);
        lirrFareMap.put("9to1", 16.75f);
        lirrFareMap.put("10to1", 19.75f);
        lirrFareMap.put("12to1", 23.5f);
        lirrFareMap.put("14to1", 30.5f);

        // Zone 3
        lirrFareMap.put("3to3", 5.5f);
        lirrFareMap.put("4to3", 8.25f);
        lirrFareMap.put("7to3", 10f);
        lirrFareMap.put("9to3", 12.25f);
        lirrFareMap.put("10to3", 15.25f);
        lirrFareMap.put("12to3", 20.25f);
        lirrFareMap.put("14to3", 26f);

        // Zone 4
        lirrFareMap.put("4to4", 3.25f);
        lirrFareMap.put("7to4", 3.25f);
        lirrFareMap.put("9to4", 6f);
        lirrFareMap.put("10to4", 7.5f);
        lirrFareMap.put("12to4", 11.25f);
        lirrFareMap.put("14to4", 17.75f);

        // Zone 7
        lirrFareMap.put("7to7", 3.25f);
        lirrFareMap.put("9to7", 3.25f);
        lirrFareMap.put("10to7", 6f);
        lirrFareMap.put("12to7", 10f);
        lirrFareMap.put("14to7", 16.5f);

        // Zone 9
        lirrFareMap.put("9to9", 3.25f);
        lirrFareMap.put("10to9", 3.25f);
        lirrFareMap.put("12to9", 7.5f);
        lirrFareMap.put("14to9", 13.5f);

        // Zone 10
        lirrFareMap.put("10to10", 3.25f);
        lirrFareMap.put("12to10", 3.25f);
        lirrFareMap.put("14to10", 9.5f);

        // Zone 12
        lirrFareMap.put("12to12", 3.25f);
        lirrFareMap.put("14to12", 6.5f);

        // Zone 14
        lirrFareMap.put("14to14", 3.25f);

        for (HashMap.Entry<String, Float> entry : lirrFareMap.entrySet()) {
            String key = entry.getKey();
            Float value = entry.getValue();
            String startZone = key.split("to")[0];
            String endZone = key.split("to")[1];
            NycAgencyFare lirrFare= new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.peak_hour_only, value.floatValue(), startZone, endZone,null);
            agencyFares.put(lirrFare.getKey(), lirrFare);
            if(!endZone.equals(startZone)) {
                NycAgencyFare olirrFare= new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.peak_hour_only, value.floatValue(), endZone, startZone,null);
                agencyFares.put(olirrFare.getKey(), olirrFare);
            }
        }


        //////
        // LIRR Off Peak In-Direct Routes
        //////

        lirrFareMap.clear();

        // via Woodside Station (ZONE 1) (Page 12-5)
        lirrFareMap.put("1to1to1", 7.25f);
        lirrFareMap.put("3to1to1", 8.25f);
        lirrFareMap.put("4to1to1", 9.5f);
        lirrFareMap.put("7to1to1", 10.5f);
        lirrFareMap.put("9to1to1", 12.5f);
        lirrFareMap.put("10to1to1", 24.75f);
        lirrFareMap.put("12to1to1", 18f);
        lirrFareMap.put("14to1to1", 22.5f);

        lirrFareMap.put("3to1to3", 8.75f);
        lirrFareMap.put("4to1to3", 9.75f);
        lirrFareMap.put("7to1to3", 11.5f);
        lirrFareMap.put("9to1to3", 13.25f);
        lirrFareMap.put("10to1to3", 15f);
        lirrFareMap.put("12to1to3", 18.25f);
        lirrFareMap.put("14to1to3", 23.25f);

        lirrFareMap.put("4to1to4", 10.75f);
        lirrFareMap.put("7to1to4", 12.5f);
        lirrFareMap.put("9to1to4", 14.25f);
        lirrFareMap.put("10to1to4", 26.25f);
        lirrFareMap.put("12to1to4", 19.75f);
        lirrFareMap.put("14to1to4", 24.5f);


        // via Jamaica Station (ZONE 3) (Page 12-6)
        lirrFareMap.put("1to3to1", 8.75f);

        lirrFareMap.put("3to3to3", 4f);
        lirrFareMap.put("4to3to3", 6.5f);
        lirrFareMap.put("7to3to3", 7.75f);
        lirrFareMap.put("9to3to3", 9.5f);
        lirrFareMap.put("10to3to3", 11.5f);
        lirrFareMap.put("12to3to3", 15f);
        lirrFareMap.put("14to3to3", 19.75f);

        lirrFareMap.put("4to3to4", 7.25f);
        lirrFareMap.put("7to3to4", 8f);
        lirrFareMap.put("9to3to4", 9.75f);
        lirrFareMap.put("10to3to4", 12f);
        lirrFareMap.put("12to3to4", 15.5f);
        lirrFareMap.put("14to3to4", 20.25f);

        lirrFareMap.put("7to3to7", 9f);
        lirrFareMap.put("9to3to7", 11.5f);
        lirrFareMap.put("10to3to7", 13.25f);
        lirrFareMap.put("12to3to7", 16.5f);
        lirrFareMap.put("14to3to7", 21.75f);

        lirrFareMap.put("9to3to9", 13.5f);
        lirrFareMap.put("10to3to9", 15.5f);
        lirrFareMap.put("12to3to9", 19f);
        lirrFareMap.put("14to3to9", 23.75f);

        lirrFareMap.put("10to3to10", 16.5f);
        lirrFareMap.put("12to3to10", 20.25f);
        lirrFareMap.put("14to3to10", 25.25f);

        lirrFareMap.put("12to3to12", 24f);
        lirrFareMap.put("14to3to12", 29f);

        lirrFareMap.put("14to3to14", 31.75f);

        // via Lynbrook, Mineola, Valley Stream (ZONE 4) (Page 12-7)
        lirrFareMap.put("4to4to4", 4.25f);
        lirrFareMap.put("7to4to4", 4.25f);
        lirrFareMap.put("9to4to4", 6f);
        lirrFareMap.put("10to4to4", 8.25f);
        lirrFareMap.put("12to4to4", 12f);
        lirrFareMap.put("14to4to4", 18.5f);

        lirrFareMap.put("7to4to7", 4.5f);
        lirrFareMap.put("9to4to7", 6.5f);
        lirrFareMap.put("10to4to7", 9f);
        lirrFareMap.put("12to4to7", 12.25f);
        lirrFareMap.put("14to4to7", 18.75f);

        lirrFareMap.put("9to4to9", 9.25f);
        lirrFareMap.put("10to4to9", 11f);
        lirrFareMap.put("12to4to9", 14.75f);
        lirrFareMap.put("14to4to9", 21.75f);

        lirrFareMap.put("10to4to10", 13.5f);
        lirrFareMap.put("12to4to10", 17.75f);
        lirrFareMap.put("14to4to10", 24.5f);

        lirrFareMap.put("12to4to12", 21.75f);
        lirrFareMap.put("14to4to12", 28f);

        lirrFareMap.put("14to4to14", 30.75f);

        // via Hicksville (ZONE 7) (Page 12-8)
        lirrFareMap.put("7to7to7", 4.25f);
        lirrFareMap.put("9to7to7", 4.25f);
        lirrFareMap.put("10to7to7", 6f);
        lirrFareMap.put("12to7to7", 10.5f);
        lirrFareMap.put("14to7to7", 17.25f);

        lirrFareMap.put("9to7to9", 4.5f);
        lirrFareMap.put("10to7to9", 6.5f);
        lirrFareMap.put("12to7to9", 11f);
        lirrFareMap.put("14to7to9", 17.75f);

        lirrFareMap.put("10to7to10", 10f);
        lirrFareMap.put("12to7to10", 13.5f);
        lirrFareMap.put("14to7to10", 20.25f);

        lirrFareMap.put("12to7to12", 17.75f);
        lirrFareMap.put("14to7to12", 24.5f);

        lirrFareMap.put("14to7to14", 28f);


        for (HashMap.Entry<String, Float> entry : lirrFareMap.entrySet()) {
            String key = entry.getKey();
            Float value = entry.getValue();
            String startZone = key.split("to")[0];
            String midZone = key.split("to")[1];
            String endZone = key.split("to")[2];
            NycAgencyFare lirrFare= new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.non_peak_hour_only, value.floatValue(), startZone, endZone, midZone);
            agencyFares.put(lirrFare.getKey(), lirrFare);
            if(!endZone.equals(startZone)) {
                NycAgencyFare reverseLirrFare = new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.non_peak_hour_only, value.floatValue(), endZone, startZone, midZone);
                agencyFares.put(reverseLirrFare.getKey(), reverseLirrFare);
            }
        }

        //////
        // LIRR Peak In-Direct Routes
        //////

        lirrFareMap.clear();

        // via Woodside Station (ZONE 1) (Page 12-5)
        lirrFareMap.put("1to1to1", 10.25f);
        lirrFareMap.put("3to1to1", 11.5f);
        lirrFareMap.put("4to1to1", 13f);
        lirrFareMap.put("7to1to1", 14.5f);
        lirrFareMap.put("9to1to1", 17.25f);
        lirrFareMap.put("10to1to1", 20.25f);
        lirrFareMap.put("12to1to1", 24.75f);
        lirrFareMap.put("14to1to1", 31f);

        lirrFareMap.put("3to1to3", 12f);
        lirrFareMap.put("4to1to3", 13.5f);
        lirrFareMap.put("7to1to3", 15.75f);
        lirrFareMap.put("9to1to3", 18.25f);
        lirrFareMap.put("10to1to3", 20.5f);
        lirrFareMap.put("12to1to3", 25f);
        lirrFareMap.put("14to1to3", 32f);

        lirrFareMap.put("4to1to4", 14.75f);
        lirrFareMap.put("7to1to4", 17.25f);
        lirrFareMap.put("9to1to4", 19.75f);
        lirrFareMap.put("10to1to4", 22.25f);
        lirrFareMap.put("12to1to4", 27f);
        lirrFareMap.put("14to1to4", 33.75f);


        // via Jamaica Station (ZONE 3) (Page 12-6)
        lirrFareMap.put("1to3to1", 12f);

        lirrFareMap.put("3to3to3", 5.5f);
        lirrFareMap.put("4to3to3", 9f);
        lirrFareMap.put("7to3to3", 10.5f);
        lirrFareMap.put("9to3to3", 13f);
        lirrFareMap.put("10to3to3", 15.75f);
        lirrFareMap.put("12to3to3", 20.5f);
        lirrFareMap.put("14to3to3", 27f);

        lirrFareMap.put("4to3to4", 10f);
        lirrFareMap.put("7to3to4", 11f);
        lirrFareMap.put("9to3to4", 13.5f);
        lirrFareMap.put("10to3to4", 16.5f);
        lirrFareMap.put("12to3to4", 21.25f);
        lirrFareMap.put("14to3to4", 27.75f);

        lirrFareMap.put("7to3to7", 12.25f);
        lirrFareMap.put("9to3to7", 15.75f);
        lirrFareMap.put("10to3to7", 18.25f);
        lirrFareMap.put("12to3to7", 22.75f);
        lirrFareMap.put("14to3to7", 29.75f);

        lirrFareMap.put("9to3to9", 18.5f);
        lirrFareMap.put("10to3to9", 21.25f);
        lirrFareMap.put("12to3to9", 26f);
        lirrFareMap.put("14to3to9", 32.75f);

        lirrFareMap.put("10to3to10", 22.75f);
        lirrFareMap.put("12to3to10", 28f);
        lirrFareMap.put("14to3to10", 34.75f);

        lirrFareMap.put("12to3to12", 33f);
        lirrFareMap.put("14to3to12", 40f);

        lirrFareMap.put("14to3to14", 43.5f);

        // via Lynbrook, Mineola, Valley Stream (ZONE 4) (Page 12-7)
        lirrFareMap.put("4to4to4", 4.25f);
        lirrFareMap.put("7to4to4", 4.25f);
        lirrFareMap.put("9to4to4", 6f);
        lirrFareMap.put("10to4to4", 8.25f);
        lirrFareMap.put("12to4to4", 12f);
        lirrFareMap.put("14to4to4", 18.5f);

        lirrFareMap.put("7to4to7", 4.5f);
        lirrFareMap.put("9to4to7", 6.5f);
        lirrFareMap.put("10to4to7", 9f);
        lirrFareMap.put("12to4to7", 12.25f);
        lirrFareMap.put("14to4to7", 18.75f);

        lirrFareMap.put("9to4to9", 9.25f);
        lirrFareMap.put("10to4to9", 11f);
        lirrFareMap.put("12to4to9", 14.75f);
        lirrFareMap.put("14to4to9", 21.75f);

        lirrFareMap.put("10to4to10", 13.5f);
        lirrFareMap.put("12to4to10", 17.75f);
        lirrFareMap.put("14to4to10", 24.5f);

        lirrFareMap.put("12to4to12", 21.75f);
        lirrFareMap.put("14to4to12", 28f);

        lirrFareMap.put("14to4to14", 30.75f);

        // via Hicksville (ZONE 7) (Page 12-8)
        lirrFareMap.put("7to7to7", 4.25f);
        lirrFareMap.put("9to7to7", 4.25f);
        lirrFareMap.put("10to7to7", 6f);
        lirrFareMap.put("12to7to7", 10.5f);
        lirrFareMap.put("14to7to7", 17.25f);

        lirrFareMap.put("9to7to9", 4.5f);
        lirrFareMap.put("10to7to9", 6.5f);
        lirrFareMap.put("12to7to9", 11f);
        lirrFareMap.put("14to7to9", 17.75f);

        lirrFareMap.put("10to7to10", 10f);
        lirrFareMap.put("12to7to10", 13.5f);
        lirrFareMap.put("14to7to10", 20.25f);

        lirrFareMap.put("12to7to12", 17.75f);
        lirrFareMap.put("14to7to12", 24.5f);

        lirrFareMap.put("14to7to14", 28f);


        for (HashMap.Entry<String, Float> entry : lirrFareMap.entrySet()) {
            String key = entry.getKey();
            Float value = entry.getValue();
            String startZone = key.split("to")[0];
            String midZone = key.split("to")[1];
            String endZone = key.split("to")[2];
            NycAgencyFare lirrFare= new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.peak_hour_only, value.floatValue(), startZone, endZone, midZone);
            agencyFares.put(lirrFare.getKey(), lirrFare);
            if(!endZone.equals(startZone)) {
                NycAgencyFare reverseLirrFare = new NycAgencyFare(lirr, FareType.regular, NycFareConditionType.peak_hour_only, value.floatValue(), endZone, startZone, midZone);
                agencyFares.put(reverseLirrFare.getKey(), reverseLirrFare);
            }
        }

        ////////////////////////////
        // MNR Fares
        ////////////////////////////


        //////
        // MNR Non-Peak Fares
        //////

        HashMap<String, Float> mnrFareMap = new HashMap<String, Float>();

        // Manhattan
        mnrFareMap.put("0to1", 6.25f);
        mnrFareMap.put("0to2", 7.25f);
        mnrFareMap.put("0to3", 8.75f);
        mnrFareMap.put("0to4", 9.75f);
        mnrFareMap.put("0to5", 11.25f);
        mnrFareMap.put("0to6", 13.25f);
        mnrFareMap.put("0to7", 15f);
        mnrFareMap.put("0to8", 17.5f);
        mnrFareMap.put("0to9", 19.25f);
        mnrFareMap.put("0to10", 20.25f);

        // Zone 1
        mnrFareMap.put("1to0", 6.25f);
        mnrFareMap.put("1to2", 8.25f);
        mnrFareMap.put("1to3", 9.75f);
        mnrFareMap.put("1to4", 10.75f);
        mnrFareMap.put("1to5", 12.25f);
        mnrFareMap.put("1to6", 14.25f);
        mnrFareMap.put("1to7", 16f);
        mnrFareMap.put("1to8", 18.5f);
        mnrFareMap.put("1to9", 20.25f);
        mnrFareMap.put("1to10", 21.25f);

        // Zone 2
        mnrFareMap.put("2to0", 7.25f);
        mnrFareMap.put("2to1", 7.25f);
        mnrFareMap.put("2to2", 3f);
        mnrFareMap.put("2to3", 3f);
        mnrFareMap.put("2to4", 4.25f);
        mnrFareMap.put("2to5", 5.75f);
        mnrFareMap.put("2to6", 8f);
        mnrFareMap.put("2to7", 11.5f);
        mnrFareMap.put("2to8", 14.5f);
        mnrFareMap.put("2to9", 16.75f);
        mnrFareMap.put("2to10", 18f);

        // Zone 3
        mnrFareMap.put("3to0", 8.75f);
        mnrFareMap.put("3to1", 8.75f);
        mnrFareMap.put("3to2", 3f);
        mnrFareMap.put("3to3", 3f);
        mnrFareMap.put("3to4", 4f);
        mnrFareMap.put("3to5", 5f);
        mnrFareMap.put("3to6", 7.25f);
        mnrFareMap.put("3to7", 8.75f);
        mnrFareMap.put("3to8", 13f);
        mnrFareMap.put("3to9", 15.25f);
        mnrFareMap.put("3to10", 16f);

        // Zone 4
        mnrFareMap.put("4to0", 9.75f);
        mnrFareMap.put("4to1", 9.75f);
        mnrFareMap.put("4to2", 4.25f);
        mnrFareMap.put("4to3", 4f);
        mnrFareMap.put("4to4", 3f);
        mnrFareMap.put("4to5", 3f);
        mnrFareMap.put("4to6", 6f);
        mnrFareMap.put("4to7", 7.75f);
        mnrFareMap.put("4to8", 11.5f);
        mnrFareMap.put("4to9", 13.75f);
        mnrFareMap.put("4to10", 14.75f);

        // Zone 5
        mnrFareMap.put("5to0", 11.25f);
        mnrFareMap.put("5to1", 11.25f);
        mnrFareMap.put("5to2", 5.75f);
        mnrFareMap.put("5to3", 5f);
        mnrFareMap.put("5to4", 3f);
        mnrFareMap.put("5to5", 3f);
        mnrFareMap.put("5to6", 3f);
        mnrFareMap.put("5to7", 6.75f);
        mnrFareMap.put("5to8", 8.25f);
        mnrFareMap.put("5to9", 12f);
        mnrFareMap.put("5to10", 12.75f);

        // Zone 6
        mnrFareMap.put("6to0", 13.25f);
        mnrFareMap.put("6to1", 13.25f);
        mnrFareMap.put("6to2", 8f);
        mnrFareMap.put("6to3", 7.25f);
        mnrFareMap.put("6to4", 6f);
        mnrFareMap.put("6to5", 3f);
        mnrFareMap.put("6to6", 3f);
        mnrFareMap.put("6to7", 3f);
        mnrFareMap.put("6to8", 6.75f);
        mnrFareMap.put("6to9", 8.25f);
        mnrFareMap.put("6to10", 10f);

        // Zone 7
        mnrFareMap.put("7to0", 15f);
        mnrFareMap.put("7to1", 15f);
        mnrFareMap.put("7to2", 11.5f);
        mnrFareMap.put("7to3", 8.75f);
        mnrFareMap.put("7to4", 7.75f);
        mnrFareMap.put("7to5", 6.75f);
        mnrFareMap.put("7to6", 3f);
        mnrFareMap.put("7to7", 3f);
        mnrFareMap.put("7to8", 4.25f);
        mnrFareMap.put("7to9", 6.5f);
        mnrFareMap.put("7to10", 7.5f);

        // Zone 8
        mnrFareMap.put("8to0", 17.5f);
        mnrFareMap.put("8to1", 17.5f);
        mnrFareMap.put("8to2", 14.5f);
        mnrFareMap.put("8to3", 13f);
        mnrFareMap.put("8to4", 11.5f);
        mnrFareMap.put("8to5", 8.25f);
        mnrFareMap.put("8to6", 6.75f);
        mnrFareMap.put("8to7", 4.25f);
        mnrFareMap.put("8to8", 3f);
        mnrFareMap.put("8to9", 4.25f);
        mnrFareMap.put("8to10", 4.5f);

        // Zone 9
        mnrFareMap.put("9to0", 19.25f);
        mnrFareMap.put("9to1", 19.25f);
        mnrFareMap.put("9to2", 16.75f);
        mnrFareMap.put("9to3", 15.25f);
        mnrFareMap.put("9to4", 13.75f);
        mnrFareMap.put("9to5", 12f);
        mnrFareMap.put("9to6", 8.25f);
        mnrFareMap.put("9to7", 6.5f);
        mnrFareMap.put("9to8", 4.25f);
        mnrFareMap.put("9to9", 3f);
        mnrFareMap.put("9to10", 3f);

        // Zone 10
        mnrFareMap.put("10to0", 20.25f);
        mnrFareMap.put("10to1", 20.25f);
        mnrFareMap.put("10to2", 18f);
        mnrFareMap.put("10to3", 16f);
        mnrFareMap.put("10to4", 14.75f);
        mnrFareMap.put("10to5", 12.75f);
        mnrFareMap.put("10to6", 10f);
        mnrFareMap.put("10to7", 7.5f);
        mnrFareMap.put("10to8", 4.5f);
        mnrFareMap.put("10to9", 3f);
        mnrFareMap.put("10to10", 3f);

        for (HashMap.Entry<String, Float> entry : mnrFareMap.entrySet()) {
            String key = entry.getKey();
            Float value = entry.getValue();
            String startZone = key.split("to")[0];
            String endZone = key.split("to")[1];

            NycAgencyFare mnrFare= new NycAgencyFare(mnr, FareType.regular, NycFareConditionType.non_peak_hour_only, value.floatValue(), startZone, endZone, null);
            agencyFares.put(mnrFare.getKey(), mnrFare);
        }

        mnrFareMap.clear();

        // GCT
        mnrFareMap.put("0to1", 8.25f);
        mnrFareMap.put("0to2", 9.75f);
        mnrFareMap.put("0to3", 11.5f);
        mnrFareMap.put("0to4", 12.75f);
        mnrFareMap.put("0to5", 14.75f);
        mnrFareMap.put("0to6", 17.5f);
        mnrFareMap.put("0to7", 20f);
        mnrFareMap.put("0to8", 23f);
        mnrFareMap.put("0to9", 25.75f);
        mnrFareMap.put("0to10", 27.25f);

        // Zone 1
        mnrFareMap.put("1to0", 8.25f);
        mnrFareMap.put("1to2", 8.25f);
        mnrFareMap.put("1to3", 9.75f);
        mnrFareMap.put("1to4", 10.75f);
        mnrFareMap.put("1to5", 12.25f);
        mnrFareMap.put("1to6", 14.25f);
        mnrFareMap.put("1to7", 16f);
        mnrFareMap.put("1to8", 18.5f);
        mnrFareMap.put("1to9", 20.25f);
        mnrFareMap.put("1to10", 21.25f);

        // Zone 2
        mnrFareMap.put("2to0", 9.75f);
        mnrFareMap.put("2to1", 7.25f);
        mnrFareMap.put("2to2", 3f);
        mnrFareMap.put("2to3", 3f);
        mnrFareMap.put("2to4", 4.25f);
        mnrFareMap.put("2to5", 5.75f);
        mnrFareMap.put("2to6", 8f);
        mnrFareMap.put("2to7", 11.5f);
        mnrFareMap.put("2to8", 14.5f);
        mnrFareMap.put("2to9", 16.75f);
        mnrFareMap.put("2to10", 18f);

        // Zone 3
        mnrFareMap.put("3to0", 11.5f);
        mnrFareMap.put("3to1", 8.75f);
        mnrFareMap.put("3to2", 3f);
        mnrFareMap.put("3to3", 3f);
        mnrFareMap.put("3to4", 4f);
        mnrFareMap.put("3to5", 5f);
        mnrFareMap.put("3to6", 7.25f);
        mnrFareMap.put("3to7", 8.75f);
        mnrFareMap.put("3to8", 13f);
        mnrFareMap.put("3to9", 15.25f);
        mnrFareMap.put("3to10", 16f);

        // Zone 4
        mnrFareMap.put("4to0", 12.75f);
        mnrFareMap.put("4to1", 9.75f);
        mnrFareMap.put("4to2", 4.25f);
        mnrFareMap.put("4to3", 4f);
        mnrFareMap.put("4to4", 3f);
        mnrFareMap.put("4to5", 3f);
        mnrFareMap.put("4to6", 6f);
        mnrFareMap.put("4to7", 7.75f);
        mnrFareMap.put("4to8", 11.5f);
        mnrFareMap.put("4to9", 13.75f);
        mnrFareMap.put("4to10", 14.75f);

        // Zone 5
        mnrFareMap.put("5to0", 14.75f);
        mnrFareMap.put("5to1", 11.25f);
        mnrFareMap.put("5to2", 5.75f);
        mnrFareMap.put("5to3", 5f);
        mnrFareMap.put("5to4", 3f);
        mnrFareMap.put("5to5", 3f);
        mnrFareMap.put("5to6", 3f);
        mnrFareMap.put("5to7", 6.75f);
        mnrFareMap.put("5to8", 8.25f);
        mnrFareMap.put("5to9", 12f);
        mnrFareMap.put("5to10", 12.75f);

        // Zone 6
        mnrFareMap.put("6to0", 17.5f);
        mnrFareMap.put("6to1", 13.25f);
        mnrFareMap.put("6to2", 8f);
        mnrFareMap.put("6to3", 7.25f);
        mnrFareMap.put("6to4", 6f);
        mnrFareMap.put("6to5", 3f);
        mnrFareMap.put("6to6", 3f);
        mnrFareMap.put("6to7", 3f);
        mnrFareMap.put("6to8", 6.75f);
        mnrFareMap.put("6to9", 8.25f);
        mnrFareMap.put("6to10", 10f);

        // Zone 7
        mnrFareMap.put("7to0", 20f);
        mnrFareMap.put("7to1", 15f);
        mnrFareMap.put("7to2", 11.5f);
        mnrFareMap.put("7to3", 8.75f);
        mnrFareMap.put("7to4", 7.75f);
        mnrFareMap.put("7to5", 6.75f);
        mnrFareMap.put("7to6", 3f);
        mnrFareMap.put("7to7", 3f);
        mnrFareMap.put("7to8", 4.25f);
        mnrFareMap.put("7to9", 6.5f);
        mnrFareMap.put("7to10", 7.5f);

        // Zone 8
        mnrFareMap.put("8to0", 23f);
        mnrFareMap.put("8to1", 17.5f);
        mnrFareMap.put("8to2", 14.5f);
        mnrFareMap.put("8to3", 13f);
        mnrFareMap.put("8to4", 11.5f);
        mnrFareMap.put("8to5", 8.25f);
        mnrFareMap.put("8to6", 6.75f);
        mnrFareMap.put("8to7", 4.25f);
        mnrFareMap.put("8to8", 3f);
        mnrFareMap.put("8to9", 4.25f);
        mnrFareMap.put("8to10", 4.5f);

        // Zone 9
        mnrFareMap.put("9to0", 25.75f);
        mnrFareMap.put("9to1", 19.25f);
        mnrFareMap.put("9to2", 16.75f);
        mnrFareMap.put("9to3", 15.25f);
        mnrFareMap.put("9to4", 13.75f);
        mnrFareMap.put("9to5", 12f);
        mnrFareMap.put("9to6", 8.25f);
        mnrFareMap.put("9to7", 6.5f);
        mnrFareMap.put("9to8", 4.25f);
        mnrFareMap.put("9to9", 3f);
        mnrFareMap.put("9to10", 3f);

        // Zone 10
        mnrFareMap.put("10to0", 27.25f);
        mnrFareMap.put("10to1", 20.25f);
        mnrFareMap.put("10to2", 18f);
        mnrFareMap.put("10to3", 16f);
        mnrFareMap.put("10to4", 14.75f);
        mnrFareMap.put("10to5", 12.75f);
        mnrFareMap.put("10to6", 10f);
        mnrFareMap.put("10to7", 7.5f);
        mnrFareMap.put("10to8", 4.5f);
        mnrFareMap.put("10to9", 3f);
        mnrFareMap.put("10to10", 3f);

        for (HashMap.Entry<String, Float> entry : mnrFareMap.entrySet()) {
            String key = entry.getKey();
            Float value = entry.getValue();
            String startZone = key.split("to")[0];
            String endZone = key.split("to")[1];

            NycAgencyFare mnrFare= new NycAgencyFare(mnr, FareType.regular, NycFareConditionType.peak_hour_only, value.floatValue(), startZone, endZone, null);
            agencyFares.put(mnrFare.getKey(), mnrFare);
        }



        ////////////////////////////////////////////////////////
        // TRANSFER RULES
        ///////////////////////////////////////////////////////

        //////////
        // LIRR Transfer Rules
        //////////
        NycTransferRule nyctLirrToLirr = new NycTransferRule(lirr, lirr, NycTransferType.merge,-1);
        transferRules.put(nyctLirrToLirr.getKey(), nyctLirrToLirr);

        //////////
        // MNRTransfer Rules
        //////////
        NycTransferRule nyctMnrToMnr = new NycTransferRule(mnr, mnr, NycTransferType.merge,-1);
        transferRules.put(nyctMnrToMnr.getKey(), nyctMnrToMnr);

        //////////
        // Bus and Subway Transfer Rules
        //////////

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

        ////////////////////////////////////////////////////////
        // Peak Hours
        ///////////////////////////////////////////////////////

        // Bus and Subway Peak Hours
        Integer[] weekdays = {1,2,3,4,5};
        Integer[] hours = {6,7,8,9,10,15,16,17,18,19};
        NycAgencyPeakHour nyctPeakHours = new NycAgencyPeakHour(nyctExpressBus, weekdays, hours, false);
        NycAgencyPeakHour mtabcPeakHours = new NycAgencyPeakHour(mtabcExpressBus, weekdays, hours, false);
        agencyPeakHours.put(nyctPeakHours.getKey(), nyctPeakHours);
        agencyPeakHours.put(mtabcPeakHours.getKey(), mtabcPeakHours);

        // LIRR Peak hours
        NycAgencyPeakHour lirrPeakHours = new NycAgencyPeakHour(lirr, null, null, true);
        agencyPeakHours.put(lirrPeakHours.getKey(), lirrPeakHours);

        // MNR Peak hours
        NycAgencyPeakHour mnrPeakHours = new NycAgencyPeakHour(mnr, null, null, true);
        agencyPeakHours.put(mnrPeakHours.getKey(), mnrPeakHours);
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
        RideChain newChain = new RideChain();
        newChain.isPeak = false;

        for (State state : states) {
            Edge backEdge = state.getBackEdge();
            if (backEdge instanceof StreetEdge) {
                if (newRide == null || !newRide.classifier.equals(WALK)) {
                    if (rides.size() == 0 || !rides.get(rides.size() - 1).classifier.equals(WALK)) {
                        newRide = new Ride();
                        newRide.classifier = WALK;
                        newRide.chain = newChain;
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
                    newRide.chain = newChain;
                    rides.add(newRide);

                    newRide.firstStop = ((HopEdge) backEdge).getBeginStop();
                    newRide.startZone = newRide.firstStop.getZoneId();
                    newRide.route = routeId;
                    Trip trip = state.getBackTrip();
                    newRide.gtfsTrip = trip;
                    Route route = trip.getRoute();
                    int type = route.getType();
                    newRide.routeType = type;
                    NycServiceId serviceId = new NycServiceId(agencyId, type);
                    newRide.classifier = serviceId;
                    newRide.startTime = state.getTimeSeconds();
                }
                newRide.lastStop = ((HopEdge) backEdge).getEndStop();
                newRide.endZone = newRide.lastStop.getZoneId();


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
                boolean mergeZone = false;
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
                            case merge:
                                // If we found a merge zone transfer, then set the traveled Service origin for future reference.
                                Ride lastRide = null;

                                int index = rides.indexOf(ride);
                                if(index > 0)
                                  lastRide = rides.get(index-1);

                                if(traveledService.startZone == null){
                                    traveledService.startZone = lastRide.startZone;
                                    traveledService.midZone = lastRide.endZone;
                                }
                                ride.mergeStartZone = traveledService.startZone;

                                //Look for indirect route patterns
                                if(lastRide != null) {
                                    //if (isLessThan(lastRide.endZone, traveledService.midZone)) {
                                    //    traveledService.midZone = lastRide.endZone;
                                    //}
                                    if ((isLessThan(lastRide.endZone, traveledService.midZone) && isLessThan(traveledService.midZone, ride.mergeStartZone)) ||
                                            (isGreaterThan(lastRide.endZone, traveledService.midZone) && isGreaterThan(traveledService.midZone, ride.mergeStartZone))) {
                                        traveledService.midZone = lastRide.endZone;
                                    }
                                }
                                ride.mergeMidZone = traveledService.midZone;

                                agencyFare = findAgencyFare(ride, fareType);
                                transferFare = agencyFare.price - traveledService.price;
                                traveledService.price = agencyFare.price;
                                transferFound = true;
                                mergeZone = true;
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

                    // One transfer is allowed in one trip (unless using merge zone rules)
                    // remove the service that covers the transfer to current ride
                    if (targetPrevTraveledService != null && !mergeZone) {
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
        if(ride.mergeStartZone != null && !ride.mergeStartZone.isEmpty()) {
            zoneKey += '_' + ride.mergeStartZone;
        }
        else if(ride.startZone != null && !ride.startZone.isEmpty()) {
            zoneKey += '_' + ride.startZone;
        }
        if(ride.mergeMidZone != null && !ride.mergeMidZone.isEmpty()
                && ((isLessThan(ride.mergeMidZone,ride.mergeStartZone) && isLessThan(ride.mergeMidZone, ride.endZone))
                    || (isGreaterThan(ride.mergeMidZone,ride.mergeStartZone) && isGreaterThan(ride.mergeMidZone, ride.endZone)))){
            zoneKey += '_' + ride.mergeMidZone;
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
                    break;
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
        if(ride.chain.isPeak){
            return true;
        }
        String ruleKey = ride.agency + "_" + ride.routeType;
        NycAgencyPeakHour peakHours = agencyPeakHours.get(ruleKey);

        // no peak hour rules for this service
        if (peakHours == null) {
            return false;
        }

        // if GTFS defines peak or off peak
        if(peakHours.useGtfs) {
            if (ride.gtfsTrip.getPeakOffpeak() == 1) {
                ride.chain.isPeak = true;
                return true;
            }
            else
                return false;
        }

        // if we manually check the hours
        long rideTimeSeconds = ride.startTime;
        Date rideTime = new Date(rideTimeSeconds * 1000);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(rideTime);
        Integer dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        Integer hour = calendar.get(Calendar.HOUR_OF_DAY);

        //List<int> days = Arrays.asList(peakHours.days);
        if(Arrays.asList(peakHours.days).contains(dayOfWeek) && Arrays.asList(peakHours.hours).contains(hour)) {
            ride.chain.isPeak = true;
            return true;
        }
        else
            return false;
    }


    /** check to see if zone A is less than zone B */
    private boolean isLessThan(String zoneA, String zoneB){
        return Integer.parseInt(zoneA) < Integer.parseInt(zoneB);
    }

    /** check to see if zone A is less than zone B */
    private boolean isGreaterThan(String zoneA, String zoneB){
        return Integer.parseInt(zoneA) > Integer.parseInt(zoneB);
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
