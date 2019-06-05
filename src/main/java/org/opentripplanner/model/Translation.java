/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Translation extends IdentityBean<Integer> {

    private static final long serialVersionUID = 1L;

    private int id;

    private String tableName;

    private String fieldName;

    private String language;

    private String translation;

    private String recordId;

    private String recordSubId;

    private String fieldValue;

    public Translation() {
    }

    public Translation(Translation t) {
        this.id = t.id;
        this.tableName = t.tableName;
        this.fieldName = t.fieldName;
        this.language = t.language;
        this.translation = t.translation;
        this.recordId = t.recordId;
        this.recordSubId = t.recordSubId;
        this.fieldValue = t.fieldValue;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getRecordSubId() {
        return recordSubId;
    }

    public void setRecordSubId(String recordSubId) {
        this.recordSubId = recordSubId;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
