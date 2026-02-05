package com.framework.model;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data = new HashMap<>();

    public ModelView(String view) {
        this.view = view;
    }

    public String getView() { return view; }
    public Map<String, Object> getData() { return data; }

    public void addAttribute(String key, Object value) {
        data.put(key, value);
    }
}



