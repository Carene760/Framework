package com.framework.model;

import java.util.Arrays;

public class UserSession {
    private Object user;
    private String[] roles;
    private boolean authenticated;

    public UserSession() {
        this.roles = new String[0];
        this.authenticated = false;
    }

    public UserSession(Object user, String[] roles, boolean authenticated) {
        this.user = user;
        this.roles = roles != null ? roles : new String[0];
        this.authenticated = authenticated;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }

    public String[] getRole() {
        return roles;
    }

    public void setRole(String[] roles) {
        this.roles = roles != null ? roles : new String[0];
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Boolean hasRole() {
        return roles != null && roles.length > 0;
    }

    public boolean hasRole(String expectedRole) {
        if (expectedRole == null || expectedRole.trim().isEmpty() || roles == null) {
            return false;
        }
        String normalized = expectedRole.trim();
        return Arrays.stream(roles)
                .filter(r -> r != null && !r.trim().isEmpty())
                .anyMatch(r -> r.trim().equalsIgnoreCase(normalized));
    }

    public boolean hasAnyRole(String[] expectedRoles) {
        if (expectedRoles == null || expectedRoles.length == 0) {
            return true;
        }
        for (String expectedRole : expectedRoles) {
            if (hasRole(expectedRole)) {
                return true;
            }
        }
        return false;
    }
}
