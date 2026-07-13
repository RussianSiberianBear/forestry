package com.alhrb.forestry.user;

public enum Role {
    SUPERADMIN,ADMIN,USER;
    public static Role getByIndex(int index) {
        if (index < 0 || index >= values().length) {
            throw new IllegalArgumentException("Invalid index: " + index +
                    ". Must be between 0 and " + (values().length - 1));
        }
        return values()[index];
    }
}