package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Translation;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Map from the OBA model of GTFS-flex areas to the OTP internal model of areas. */
class TranslationMapper {

    private final Map<org.onebusaway.gtfs.model.Translation, Translation> mappedTranslations = new HashMap<>();

    Collection<Translation> map(Collection<org.onebusaway.gtfs.model.Translation> translations) {
        return MapUtils.mapToList(translations, this::map);
    }

    /** Map from the OBA model of GTFS-flex areas to the OTP internal model of areas.  */
    Translation map(org.onebusaway.gtfs.model.Translation orginal) {
        return orginal == null ? null : mappedTranslations.computeIfAbsent(orginal, this::doMap);
    }

    private Translation doMap(org.onebusaway.gtfs.model.Translation rhs) {
        Translation lhs = new Translation();

        lhs.setId(rhs.getId());
        lhs.setTableName(rhs.getTableName());
        lhs.setFieldName(rhs.getFieldName());
        lhs.setLanguage(rhs.getLanguage());
        lhs.setTranslation(rhs.getTranslation());
        lhs.setRecordId(rhs.getRecordId());
        lhs.setRecordSubId(rhs.getRecordSubId());
        lhs.setFieldValue(rhs.getFieldValue());

        return lhs;
    }
}