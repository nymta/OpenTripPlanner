/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.translation;

import org.opentripplanner.model.Translation;

import java.io.Serializable;

public class PropertyTranslation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String propertyName;

    private String translation;

    private String entityId;

    private String entitySubId;

    private String propertyValue;

    public PropertyTranslation(String propertyName, Translation translation) {
        this.propertyName = propertyName;
        this.translation = translation.getTranslation();
        this.entityId = translation.getRecordId();
        this.entitySubId = translation.getRecordSubId();
        this.propertyValue = translation.getFieldValue();
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntitySubId() {
        return entitySubId;
    }

    public void setEntitySubId(String entitySubId) {
        this.entitySubId = entitySubId;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }
}
