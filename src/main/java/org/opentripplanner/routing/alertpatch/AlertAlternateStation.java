package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.util.I18NString;

/**
 * Represent additional info in an alert representing possible alternate station.
 */
public class AlertAlternateStation {

    private I18NString notes;
    private String agencyId;
    private String stopId;

    public I18NString getNotes() {
        return notes;
    }

    public void setNotes(I18NString notes) {
        this.notes = notes;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AlertAlternateStation)) {
            return false;
        }
        AlertAlternateStation aas = (AlertAlternateStation) o;
        if (notes == null) {
            if (aas.notes != null) {
                return false;
            }
        } else {
            if (!notes.equals(aas.notes)) {
                return false;
            }
        }
        if (agencyId == null) {
            if (aas.agencyId != null) {
                return false;
            }
        } else {
            if (!agencyId.equals(aas.agencyId)) {
                return false;
            }
        }
        if (stopId == null) {
            return aas.stopId == null;
        } else {
            return (!stopId.equals(aas.stopId));
        }
    }

    public int hashCode() {
        return (agencyId == null ? 0 : agencyId.hashCode())
                + (stopId == null ? 0 : stopId.hashCode())
                + (notes == null ? 0 : notes.hashCode());
    }

}
