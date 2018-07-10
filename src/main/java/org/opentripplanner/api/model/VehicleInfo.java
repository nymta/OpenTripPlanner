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
package org.opentripplanner.api.model;

public class VehicleInfo {

    public enum StopStatus { INCOMING_AT, STOPPED_AT, IN_TRANSIT_TO };

    private String vehicleId;

    private String vehicleLabel;

    private double lat;

    private double lon;

    private String stop;

    private int currentStopSequence;

    private StopStatus currentStopStatus;

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public void setVehicleLabel(String vehicleLabel) {
        this.vehicleLabel = vehicleLabel;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getStop() {
        return stop;
    }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public int getCurrentStopSequence() {
        return currentStopSequence;
    }

    public void setCurrentStopSequence(int currentStopSequence) {
        this.currentStopSequence = currentStopSequence;
    }

    public StopStatus getCurrentStopStatus() {
        return currentStopStatus;
    }

    public void setCurrentStopStatus(StopStatus currentStopStatus) {
        this.currentStopStatus = currentStopStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VehicleInfo that = (VehicleInfo) o;

        if (Double.compare(that.lat, lat) != 0) return false;
        if (Double.compare(that.lon, lon) != 0) return false;
        if (currentStopSequence != that.currentStopSequence) return false;
        if (vehicleId != null ? !vehicleId.equals(that.vehicleId) : that.vehicleId != null) return false;
        if (vehicleLabel != null ? !vehicleLabel.equals(that.vehicleLabel) : that.vehicleLabel != null) return false;
        if (stop != null ? !stop.equals(that.stop) : that.stop != null) return false;
        return currentStopStatus == that.currentStopStatus;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = vehicleId != null ? vehicleId.hashCode() : 0;
        result = 31 * result + (vehicleLabel != null ? vehicleLabel.hashCode() : 0);
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (stop != null ? stop.hashCode() : 0);
        result = 31 * result + currentStopSequence;
        result = 31 * result + (currentStopStatus != null ? currentStopStatus.hashCode() : 0);
        return result;
    }
}
