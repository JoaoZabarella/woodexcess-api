package com.z.c.woodexcess_api.enums;

public enum UserRole {
    ADMIN("Admin"),
    USER("User");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
