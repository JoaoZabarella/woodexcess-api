package com.z.c.woodexcess_api.enums;

public enum MaterialType {
    WOOD("Madeira"),
    MDF("MDF"),
    PLYWOOD("Compensado"),
    VENEER("Laminado"),
    PARTICLE_BOARD("Aglomerado"),
    OTHER("Outros");

    private final String displayName;

    MaterialType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
