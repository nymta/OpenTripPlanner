package org.opentripplanner.api.model.alertpatch;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.routing.alertpatch.AlertAlternateStation;

import java.util.Locale;

public class LocalizedAlertAlternateStation {

    private AlertAlternateStation alternate;
    private Locale locale;

    public LocalizedAlertAlternateStation(AlertAlternateStation aas, Locale locale) {
        this.alternate = aas;
        this.locale = locale;
    }

    public LocalizedAlertAlternateStation() {

    }

    @JsonSerialize
    public String getNotes() {
        if (alternate.getNotes() == null) {
            return null;
        }
        return alternate.getNotes().toString(locale);
    }

    @JsonSerialize
    public String getAgencyId() {
        return alternate.getAgencyId();
    }

    @JsonSerialize
    public String getStopId() {
        return alternate.getStopId();
    }
}
