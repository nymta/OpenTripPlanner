package org.opentripplanner.translation;

import org.opentripplanner.model.translation.PropertyTranslation;
import org.opentripplanner.model.translation.TranslationServiceData;
import org.opentripplanner.model.translation.TypeAndLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is actually kind of a hack, and assumes that there is only one copy of TranslationServiceData
 * in the universe.
 */
public class MultiTranslationServiceImpl extends TranslationServiceImpl {

    public MultiTranslationServiceImpl() {
        setData(new TranslationServiceData());
    }

    public void addData(TranslationServiceData data) {
        if (data == null) {
            return;
        }
        TranslationServiceData _data = super.getData();
        // Note this updates the feed language. Similar to timezone, unclear what the proper behavior should be here.
        _data.setFeedLanguage(data.getFeedLanguage());
        for (Map.Entry<TypeAndLanguage, List<PropertyTranslation>> entry : data.getTranslationMap().entrySet()) {
            // key/value may already exist
            List<PropertyTranslation> translations = _data.getTranslationMap()
                    .computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
            translations.addAll(entry.getValue());
        }
    }

}
