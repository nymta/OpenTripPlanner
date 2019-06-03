package org.opentripplanner.api.flex;

import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.TripShort;
import org.opentripplanner.model.Agency;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class FlexService {

    private Agency agency;

    private RouteShort route;

    private TripShort trip;

    private String serviceInfo;

    private String days;

    private String startTime;

    private String endTime;

    private EncodedPolylineBean serviceArea;

    private Double serviceAreaRadius;

    private boolean deviatedService;

    public Agency getAgency() {
        return agency;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    public RouteShort getRoute() {
        return route;
    }

    public void setRoute(RouteShort route) {
        this.route = route;
    }

    public TripShort getTrip() {
        return trip;
    }

    public void setTrip(TripShort trip) {
        this.trip = trip;
    }

    public String getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(String serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public EncodedPolylineBean getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(EncodedPolylineBean serviceArea) {
        this.serviceArea = serviceArea;
    }

    public Double getServiceAreaRadius() {
        return serviceAreaRadius;
    }

    public void setServiceAreaRadius(Double serviceAreaRadius) {
        this.serviceAreaRadius = serviceAreaRadius;
    }

    public boolean isDeviatedService() {
        return deviatedService;
    }

    public void setDeviatedService(boolean deviatedService) {
        this.deviatedService = deviatedService;
    }
}
