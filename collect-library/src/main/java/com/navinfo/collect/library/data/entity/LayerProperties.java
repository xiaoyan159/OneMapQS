package com.navinfo.collect.library.data.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class LayerProperties {
    private String name;
    private int fieldType = FieldType.STRING.getTypeCode(); /*默认是字符串类型*/

    public LayerProperties() {
    }

    public enum FieldType {
        STRING(0),  NUM(1);
        int typeCode;

        FieldType(int typeCode) {
            this.typeCode = typeCode;
        }

        public int getTypeCode() {
            return typeCode;
        }

        public FieldType getFieldType(int typeCode) {
            for (FieldType fieldType: FieldType.values()) {
                if (fieldType.getTypeCode() == typeCode) {
                    return fieldType;
                }
            }
            return FieldType.STRING;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldType getFieldType() {
        FieldType fieldType = FieldType.STRING;
        try {
            fieldType = fieldType.getFieldType(this.fieldType);
        } catch (Exception e) {
        }
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType.typeCode;
    }
}
