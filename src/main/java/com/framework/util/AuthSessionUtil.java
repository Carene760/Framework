package com.framework.util;

import com.framework.annotation.Auth;
import com.framework.model.UserSession;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AuthSessionUtil {

    public static final String DEFAULT_USER_KEY = "username";
    public static final String DEFAULT_ROLE_KEY = "role";
    public static final String DEFAULT_AUTH_KEY = "isAuthenticated";

    public static UserSession resolveUserSession(HttpServletRequest request, ServletConfig config) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) {
            return new UserSession(null, new String[0], false);
        }

        String userKey = getInitParam(config, "auth-session-user-key", DEFAULT_USER_KEY);
        String roleKey = getInitParam(config, "auth-session-role-key", DEFAULT_ROLE_KEY);
        String authKey = getInitParam(config, "auth-session-authenticated-key", DEFAULT_AUTH_KEY);

        Object user = httpSession.getAttribute(userKey);
        Object roleValue = httpSession.getAttribute(roleKey);
        Object authValue = httpSession.getAttribute(authKey);

        String[] roles = convertToRoles(roleValue);
        boolean authenticated = resolveAuthenticated(user, roles, authValue);

        return new UserSession(user, roles, authenticated);
    }

    public static boolean isAuthorized(Auth auth, UserSession userSession) {
        if (auth == null) {
            return true;
        }

        if (userSession == null || !userSession.isAuthenticated()) {
            return false;
        }

        String[] requiredRoles = auth.value();
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }

        return userSession.hasAnyRole(requiredRoles);
    }

    private static String getInitParam(ServletConfig config, String key, String defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        String value = config.getInitParameter(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    private static boolean resolveAuthenticated(Object user, String[] roles, Object authValue) {
        if (authValue instanceof Boolean) {
            return (Boolean) authValue;
        }
        if (authValue instanceof String) {
            return Boolean.parseBoolean((String) authValue);
        }
        return user != null || (roles != null && roles.length > 0);
    }

    private static String[] convertToRoles(Object roleValue) {
        if (roleValue == null) {
            return new String[0];
        }

        List<String> roles = new ArrayList<>();

        if (roleValue instanceof String) {
            String value = (String) roleValue;
            if (value.contains(",")) {
                for (String part : value.split(",")) {
                    addRole(roles, part);
                }
            } else {
                addRole(roles, value);
            }
        } else if (roleValue instanceof String[]) {
            for (String role : (String[]) roleValue) {
                addRole(roles, role);
            }
        } else if (roleValue instanceof Collection) {
            for (Object role : (Collection<?>) roleValue) {
                addRole(roles, role != null ? role.toString() : null);
            }
        } else if (roleValue.getClass().isArray()) {
            Object[] array = (Object[]) roleValue;
            for (Object role : array) {
                addRole(roles, role != null ? role.toString() : null);
            }
        } else {
            addRole(roles, roleValue.toString());
        }

        return roles.toArray(new String[0]);
    }

    private static void addRole(List<String> roles, String role) {
        if (role == null) {
            return;
        }
        String normalized = role.trim();
        if (!normalized.isEmpty()) {
            roles.add(normalized);
        }
    }
}
