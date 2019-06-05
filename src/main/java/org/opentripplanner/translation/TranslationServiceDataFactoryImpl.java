/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.translation;

import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.csv_entities.schema.EntitySchema;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.csv_entities.schema.SingleFieldMapping;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Translation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.translation.PropertyTranslation;
import org.opentripplanner.model.translation.TranslationService;
import org.opentripplanner.model.translation.TranslationServiceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationServiceDataFactoryImpl {

    private final Logger _log = LoggerFactory.getLogger(TranslationServiceDataFactoryImpl.class);

    private static final String AGENCY_TABLE_NAME = "agency";

    private static final String STOP_TABLE_NAME = "stops";

    private static final String ROUTE_TABLE_NAME = "routes";

    private static final String TRIP_TABLE_NAME = "trips";

    private static final String STOP_TIME_TABLE_NAME = "stop_times";

    private static final String FEED_INFO_TABLE_NAME = "feed_info";

    private OtpTransitService otpTransitService;

    public static TranslationService getTranslationService(OtpTransitService otpTransitService) {
        TranslationServiceData data = createData(otpTransitService);
        TranslationServiceImpl translationService = new TranslationServiceImpl();
        translationService.setData(data);
        return translationService;
    }

    public static TranslationServiceData createData(OtpTransitService otpTransitService) {
        TranslationServiceDataFactoryImpl factory = new TranslationServiceDataFactoryImpl();
        factory.setOtpTransitService(otpTransitService);
        return factory.getTranslationServiceData();
    }

    public void setOtpTransitService(OtpTransitService otpTransitService) {
        this.otpTransitService = otpTransitService;
    }

    public TranslationServiceData getTranslationServiceData() {
        if (otpTransitService.getAllFeedInfos().isEmpty()) {
            _log.warn("No feed_info present, there will be no translations available.");
            return null;
        }
        TranslationServiceData data = new TranslationServiceData();
        FeedInfo feedInfo = otpTransitService.getAllFeedInfos().iterator().next();
        if (feedInfo.getDefaultLang() != null) {
            data.setFeedLanguage(feedInfo.getDefaultLang());
        } else {
            data.setFeedLanguage(feedInfo.getLang());
        }
        for (Translation translation : otpTransitService.getAllTranslations()) {
            Class<?> type = getEntityTypeForTableName(translation.getTableName());
            if (type == null) {
                _log.error("No entity for table_name {}, skipping.", translation.getTableName());
                continue;
            }
            String propertyName = getPropertyNameByClassAndCsvName(type, translation.getFieldName());
            if (propertyName == null) {
                _log.error("No property for field_name {}, skipping.", translation.getFieldName());
                continue;
            }
            PropertyTranslation propertyTranslation = new PropertyTranslation(propertyName, translation);
            data.putTranslation(type, translation.getLanguage(), propertyTranslation);
        }
        return data;
    }

    private Class<?> getEntityTypeForTableName(String name) {
        switch(name) {
            case AGENCY_TABLE_NAME:
                return Agency.class;
            case STOP_TABLE_NAME:
                return Stop.class;
            case ROUTE_TABLE_NAME:
                return Route.class;
            case TRIP_TABLE_NAME:
                return Trip.class;
            case STOP_TIME_TABLE_NAME:
                return StopTime.class;
            case FEED_INFO_TABLE_NAME:
                return FeedInfo.class;
        }
        return null;
    }

    private String getPropertyNameByClassAndCsvName(Class<?> type, String csvName) {
        DefaultEntitySchemaFactory factory = GtfsEntitySchemaFactory.createEntitySchemaFactory();
        EntitySchema schema = factory.getSchema(getOneBusAwayClassByOpenTripPlannerClass(type));
        for (FieldMapping field : schema.getFields()) {
            if (field instanceof SingleFieldMapping) {
                SingleFieldMapping mapping = (SingleFieldMapping) field;
                if (csvName.equals(mapping.getCsvFieldName())) {
                    return mapping.getObjFieldName();
                }
            }
        }
        return null;
    }

    // Unfortunately, the mapping between CSV column and Java property is on the onebusaway model classes
    private Class<?> getOneBusAwayClassByOpenTripPlannerClass(Class<?> type) {
        if (Agency.class.equals(type)) {
            return org.onebusaway.gtfs.model.Agency.class;
        } else if (Stop.class.equals(type)) {
            return org.onebusaway.gtfs.model.Stop.class;
        } else if (Route.class.equals(type)) {
            return org.onebusaway.gtfs.model.Route.class;
        } else if (Trip.class.equals(type)) {
            return org.onebusaway.gtfs.model.Trip.class;
        } else if (StopTime.class.equals(type)) {
            return org.onebusaway.gtfs.model.StopTime.class;
        } else if (FeedInfo.class.equals(type)) {
            return org.onebusaway.gtfs.model.FeedInfo.class;
        } else {
            return null;
        }
    }
}
