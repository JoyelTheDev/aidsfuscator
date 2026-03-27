package dev.lvstrng.aidsfuscator.property;

import java.util.HashMap;
import java.util.Map;

public class GlobalPropertyContainer {
    private final Map<Object, PropertyContainer> properties;

    public GlobalPropertyContainer() {
        this.properties = new HashMap<>();
    }

    public <T> T add(T obj, Property... properties) {
        this.properties.computeIfAbsent(obj, _ -> new PropertyContainer()).add(properties);
        return obj;
    }

    public PropertyContainer get(Object obj) {
        // default to an empty container, because then that object has no properties (avoid null checks)
        return properties.getOrDefault(obj, new PropertyContainer());
    }

    public Map<Object, PropertyContainer> properties() {
        return properties;
    }
}
