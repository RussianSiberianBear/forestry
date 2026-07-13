package com.alhrb.forestry.user;

public enum UserRole {
    SUPERADMIN,
    ADMIN,
    USER;

    public int getIndex() {
        return ordinal();
    }

    public static UserRole getByIndex(int index) {
        if (index < 0 || index >= values().length) {
            throw new IllegalArgumentException(
                    "Invalid index: " + index +
                            ". Must be between 0 and " + (values().length - 1));
        }
        return values()[index];
    }
}