package com.company.coffee.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum DiscountType implements EnumClass<String> {

    TOTAL("TOTAL"),
    PRODUCT("PRODUCT");

    private final String id;

    DiscountType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static DiscountType fromId(String id) {
        for (DiscountType at : DiscountType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}