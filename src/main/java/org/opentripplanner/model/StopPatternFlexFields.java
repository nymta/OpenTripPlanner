package org.opentripplanner.model;

import com.vividsolutions.jts.geom.Geometry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Class to keep track of GTFS-Flex related StopPattern parameters
 */
public class StopPatternFlexFields implements Serializable {
    public final int size;

    public final int[] continuousPickup;
    public final int[] continuousDropOff;
    public final double[] serviceAreaRadius;
    public final Geometry[] serviceAreas; // likely at most one distinct object will be in this array

    /**
     * Default constructor.
     * @param stopTimes stopTimes for this pattern
     * @param areaGeometryByArea Map of GTFS-Flex areas, keyed by area ID
     */
    public StopPatternFlexFields(List<StopTime> stopTimes, Map<String, Geometry> areaGeometryByArea) {
        this (stopTimes.size());
        if (size == 0) return;
        double lastServiceAreaRadius = 0;
        Geometry lastServiceArea = null;
        for (int i = 0; i < size; ++i) {
            StopTime stopTime = stopTimes.get(i);

            // continuous stops can be empty, which means 1 (no continuous stopping behavior)
            continuousPickup[i] = stopTime.getContinuousPickup() == StopTime.MISSING_VALUE ? 1 : stopTime.getContinuousPickup();
            continuousDropOff[i] = stopTime.getContinuousDropOff() == StopTime.MISSING_VALUE ? 1 : stopTime.getContinuousDropOff();


            if (stopTime.getStartServiceAreaRadius() != StopTime.MISSING_VALUE) {
                lastServiceAreaRadius = stopTime.getStartServiceAreaRadius();
            }
            serviceAreaRadius[i] = lastServiceAreaRadius;
            if (stopTime.getEndServiceAreaRadius() != StopTime.MISSING_VALUE) {
                lastServiceAreaRadius = 0;
            }

            if (areaGeometryByArea != null) {
                if (stopTime.getStartServiceArea() != null) {
                    lastServiceArea = areaGeometryByArea.get(stopTime.getStartServiceArea().getAreaId());
                }
                serviceAreas[i] = lastServiceArea;
                if (stopTime.getEndServiceArea() != null) {
                    lastServiceArea = null;
                }
            }
        }
    }

    private StopPatternFlexFields(int size) {
        this.size = size;
        continuousPickup = new int[size];
        continuousDropOff = new int[size];
        serviceAreaRadius = new double[size];
        serviceAreas = new Geometry[size];
    }

    @Override
    public int hashCode() {
        int hash = size;
        hash += Arrays.hashCode(this.continuousPickup);
        hash *= 31;
        hash += Arrays.hashCode(this.continuousDropOff);
        hash *= 31;
        hash += Arrays.hashCode(this.serviceAreas);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopPatternFlexFields that = (StopPatternFlexFields) o;
        if (size != that.size) return false;
        if (!Arrays.equals(continuousPickup, that.continuousPickup)) return false;
        if (!Arrays.equals(continuousDropOff, that.continuousDropOff)) return false;
        if (!Arrays.equals(serviceAreaRadius, that.serviceAreaRadius)) return false;
        return Arrays.equals(serviceAreas, that.serviceAreas);
    }
}