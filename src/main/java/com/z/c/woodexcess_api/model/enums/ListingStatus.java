package com.z.c.woodexcess_api.model.enums;

public enum ListingStatus {
    ACTIVE("Ativo"),
    INACTIVE("Inativo"),
    RESERVED("Reservado"),
    SOLD("Vendido");

    private final String displayName;

    ListingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
