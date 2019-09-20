/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.translation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationServiceData implements Serializable {

    private static final long serialVersionUID = 1L;

    // Map: list of translations by entity type and language
    private Map<TypeAndLanguage, List<PropertyTranslation>> _translationMap = new HashMap<>();;

    private String _feedLanguage;

    public List<PropertyTranslation> getTranslationsByTypeAndLanguage(Class<?> type, String language) {
        return _translationMap.get(new TypeAndLanguage(type, language));
    }

    public Map<TypeAndLanguage, List<PropertyTranslation>> getTranslationMap() {
        return _translationMap;
    }

    public void putTranslation(Class<?> type, String language, PropertyTranslation translation) {
        TypeAndLanguage key = new TypeAndLanguage(type, language);
        List<PropertyTranslation> translations = _translationMap.computeIfAbsent(key, k -> new ArrayList<>());
        translations.add(translation);
    }

    public String getFeedLanguage() {
        return _feedLanguage;
    }

    public void setFeedLanguage(String feedLanguage) {
        _feedLanguage = feedLanguage;
    }
}
