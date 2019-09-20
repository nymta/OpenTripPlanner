/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.translation;

import org.onebusaway.gtfs.model.Translation;

public interface TranslationService {
    /**
     * Get a cloned version of a GTFS entity object with fields translated as per the GTFS
     * Translations proposed spec addition. See {@link Translation}
     *
     * @param language language to translate to
     * @param type entity type
     * @param instance instance to clone and translate
     * @param <T> entity type
     * @return cloned instance with the proper fields changed
     */
    <T> T getTranslatedEntity(String language, Class<T> type, T instance);
}
