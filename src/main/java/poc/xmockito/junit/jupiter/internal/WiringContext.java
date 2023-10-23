package poc.xmockito.junit.jupiter.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class WiringContext {
    enum ParameterDefinition {
        UNIQUE_BY_TYPE_AND_NAME,
        UNIQUE_BY_TYPE,
        NONUNIQUE_BY_TYPE,
        UNDEFINED
    }

    private static final Object DEFINED_NULL = new Object() {
        @Override
        public String toString() {
            return "NULL";
        }
    };
    private final Map<Type, Map<String, Object>> typeToNamedInstances = new LinkedHashMap<>();

    void clear() {
        typeToNamedInstances.clear();
    }

    void register(Field field, Object instance) {
        typeToNamedInstances.putIfAbsent(field.getType(), new LinkedHashMap<>());
        typeToNamedInstances.get(field.getType()).put(field.getName(), wrapNullAsDefinedNull(instance));
    }

    Object lookup(Class<?> type, String name) {
        return unwrapDefinedNullToNull(typeToNamedInstances.get(type).get(name));
    }

    Object lookup(Class<?> type) {
        return unwrapDefinedNullToNull(typeToNamedInstances.get(type).values().iterator().next());
    }

    Set<String> lookupNamesFor(Class<?> type) {
        return typeToNamedInstances.get(type).keySet();
    }

    public ParameterDefinition parameterDefinition(Class<?> type, String name) {
        if (isUniquelyDefinedBy(type, name)) {
            return ParameterDefinition.UNIQUE_BY_TYPE_AND_NAME;
        } else if (isUniquelyDefinedBy(type)) {
            return ParameterDefinition.UNIQUE_BY_TYPE;
        } else if (isNonUniquelyDefinedByType(type)) {
            return ParameterDefinition.NONUNIQUE_BY_TYPE;
        } else {
            return ParameterDefinition.UNDEFINED;
        }
    }

    private boolean isUniquelyDefinedBy(Class<?> type, String name) {
        return typeToNamedInstances.containsKey(type) && typeToNamedInstances.get(type).containsKey(name);
    }
    private boolean isUniquelyDefinedBy(Class<?> type) {
        return typeToNamedInstances.containsKey(type) && typeToNamedInstances.get(type).values().size() == 1;
    }

    private boolean isNonUniquelyDefinedByType(Class<?> type) {
        return typeToNamedInstances.containsKey(type) && typeToNamedInstances.get(type).values().size() > 1;
    }

    private static Object wrapNullAsDefinedNull(Object instance) {
        return instance == null ? DEFINED_NULL : instance;
    }

    private static Object unwrapDefinedNullToNull(Object instance) {
        return instance == DEFINED_NULL ? null : instance;
    }
}
