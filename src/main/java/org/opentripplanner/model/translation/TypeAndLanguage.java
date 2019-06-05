/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.translation;

import java.io.Serializable;
import java.util.Objects;

public class TypeAndLanguage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Class<?> type;

    private String language;

    TypeAndLanguage(Class<?> type, String language) {
        this.type = type;
        this.language = language;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeAndLanguage that = (TypeAndLanguage) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, language);
    }
}
