package com.framework.model;

import java.util.HashMap;
import java.util.Map;

public class SessionModelView extends ModelView {
    private Map<String, Object> sessionAttributes = new HashMap<>();
    
    public SessionModelView(String view) {
        super(view);
    }
    
    public SessionModelView addSessionAttribute(String key, Object value) {
        sessionAttributes.put(key, value);
        return this;
    }
    
    public Map<String, Object> getSessionAttributes() {
        return sessionAttributes;
    }
    
    public boolean hasSessionAttributes() {
        return !sessionAttributes.isEmpty();
    }
}



