package org.opentripplanner.translation;

import org.opentripplanner.model.translation.TranslationServiceData;

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
        _data.getTranslationMap().putAll(data.getTranslationMap());
    }

}
