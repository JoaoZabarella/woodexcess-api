package com.z.c.woodexcess_api.model.enums;

public enum Condition {
    NEW("Novo"),
    USED("Usado"),
    SCRAP("Retalho/Sobra");

    private final String displayName;

    Condition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
