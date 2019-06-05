/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.translation;

import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.BeanWrapperFactory;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.translation.PropertyTranslation;
import org.opentripplanner.model.translation.TranslationService;
import org.opentripplanner.model.translation.TranslationServiceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class implements the GTFS Translations extension proposal, documented here: http://bit.ly/gtfs-translations
 */
public class TranslationServiceImpl implements TranslationService {

    private static final Logger _log = LoggerFactory.getLogger(TranslationServiceImpl.class);

    private TranslationServiceData _data;

    public void setData(TranslationServiceData data) {
        _data = data;
    }

    public TranslationServiceData getData() {
        return _data;
    }

    @Override
    public <T> T getTranslatedEntity(String language, Class<T> type, T instance) {
        // Return the given instance if the language is the default language, or if we can't
        // initialize the translation map, or if there aren't any translations for this entity type.
        if (_data == null || language.equals(_data.getFeedLanguage())) {
            return instance;
        }
        List<PropertyTranslation> translationsForClass = _data.getTranslationsByTypeAndLanguage(type, language);
        if (translationsForClass == null || translationsForClass.isEmpty()) {
            return instance;
        }

        // Get cloned entity via typical OBA model constructor
        T translatedInstance;
        try {
            translatedInstance = type.getConstructor(type).newInstance(instance);
        } catch(Exception ex) {
            _log.error("Unable to process instance with entity type={} due to: {}", type.getName(), ex.getMessage());
            return instance;
        }

        // Wrap instance, and set translated properties if applicable.
        BeanWrapper wrapper = BeanWrapperFactory.wrap(translatedInstance);
        for (PropertyTranslation translation : translationsForClass) {
            String propertyName = translation.getPropertyName();
            String translationStr = null;
            if (objectIdMatches(translatedInstance, translation.getEntityId(), translation.getEntitySubId())) {
                translationStr = translation.getTranslation();
            } else if (translation.getPropertyValue() != null
                    && translation.getPropertyValue().equals(wrapper.getPropertyValue(propertyName))) {
                translationStr = translation.getTranslation();
            }
            if (translationStr != null) {
                wrapper.setPropertyValue(propertyName, translationStr);
            }
        }

        return wrapper.getWrappedInstance(type);
    }

    private boolean objectIdMatches(Object object, String id, String subId) {
        if (object instanceof Agency) {
            return ((Agency) object).getId().equals(id);
        } else if (object instanceof Stop) {
            return ((Stop) object).getId().getId().equals(id);
        } else if (object instanceof Route) {
            return ((Route) object).getId().getId().equals(id);
        } else if (object instanceof Trip) {
            return ((Trip) object).getId().getId().equals(id);
        } else if (object instanceof StopTime) {
            return ((StopTime) object).getTrip().getId().getId().equals(id) &&
                    ((StopTime) object).getStopSequence() == Integer.parseInt(subId);
        } else if (object instanceof FeedInfo) {
            // only one
            return true;
        }
        return false;
    }
}
