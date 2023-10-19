package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static poc.xmockito.junit.jupiter.ReflectionUtils.*;

public class XMockitoExtension implements BeforeEachCallback, AfterEachCallback {

    private final InstanceContext context = new InstanceContext();

    public void beforeEach(ExtensionContext context) {
        this.context.clear();
        Object testInstance = context.getTestInstance().get();

        // Collect Naturals
        for (Field collectedDependency : dependenciesToCollect(testInstance)) {
            this.context.registerInstance(collectedDependency, extract(testInstance, collectedDependency));
        }

        // Create Mocks
        for (Field mockedDependency : dependenciesToMock(testInstance)) {
            this.context.registerInstance(mockedDependency, Mockito.mock(mockedDependency.getType()));
        }

        // Create Instances
        for (Field field : dependenciesToInstantiate(testInstance)) {
            this.context.registerInstance(field, this.context.instantiate(field));
        }

        // Inject the created Mocks and Instances
        for (Field field : dependenciesToInject(testInstance)) {
            inject(testInstance, field, this.context.lookup(field.getType(), field.getName()));
        }
    }

    private static List<Field> dependenciesToInstantiate(Object testInstance) {
        return stream(testInstance.getClass().getDeclaredFields()).filter(it -> it.isAnnotationPresent(Instance.class)).toList();
    }

    private static List<Field> dependenciesToMock(Object testInstance) {
        return stream(testInstance.getClass().getDeclaredFields()).filter(it -> it.isAnnotationPresent(Mock.class)).toList();
    }

    private static List<Field> dependenciesToInject(Object testInstance) {
        return stream(testInstance.getClass().getDeclaredFields()).filter(it -> it.isAnnotationPresent(Instance.class) || it.isAnnotationPresent(Mock.class)).toList();
    }

    private static List<Field> dependenciesToCollect(Object testInstance) {
        return stream(testInstance.getClass().getDeclaredFields()).filter(it -> !it.isAnnotationPresent(Instance.class) && !it.isAnnotationPresent(Mock.class)).toList();
    }

    public void afterEach(ExtensionContext context) {
        this.context.clear();
    }
}

class InstanceContext {
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

    void registerInstance(Field field, Object instance) {
        typeToNamedInstances.putIfAbsent(field.getType(), new LinkedHashMap<>());
        typeToNamedInstances.get(field.getType()).put(field.getName(), wrapNullAsDefinedNull(instance));
    }

    private boolean isDefined(Class<?> type, String name) {
        return typeToNamedInstances.containsKey(type) && typeToNamedInstances.get(type).containsKey(name);
    }

    Object lookup(Class<?> type, String name) {
        return unwrapDefinedNullToNull(typeToNamedInstances.get(type).get(name));
    }

    private boolean isUniquelyDefined(Class<?> type) {
        return typeToNamedInstances.containsKey(type) && typeToNamedInstances.get(type).values().size() == 1;
    }

    private Object lookupUnique(Class<?> type) {
        return unwrapDefinedNullToNull(typeToNamedInstances.get(type).values().iterator().next());
    }

    Object instantiate(Field field) {
        try {
            Constructor<?> selectedConstructor = selectConstructor(field);
            Object[] parameters = selectedParameters(selectedConstructor);
            return selectedConstructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to instantiate %s".formatted(asString(field)), e);
        }
    }

    private Object[] selectedParameters(Constructor<?> selectedConstructor) {
        return stream(selectedConstructor.getParameters())
            .map(this::resolve)
            .toArray();
    }

    private static Constructor<?> selectConstructor(Field dependency) {
        return selectCandidateConstructor(dependency)
            .orElseThrow(() -> new RuntimeException("No matching constructor to initialize %s".formatted(asString(dependency))));
    }

    private static Optional<Constructor<?>> selectCandidateConstructor(Field dependency) {
        var constructors = dependency.getType().getConstructors();
        if (constructors.length == 0) {
            throw new RuntimeException("No public constructor to initialize %s".formatted(asString(dependency)));
        }

        var constructorSelector = constructorSelector(constructors, dependency);

        return stream(constructors).filter(constructorSelector).findFirst();
    }

    private static Predicate<Constructor<?>> constructorSelector(Constructor<?>[] constructors, Field field) {
        if (constructors.length == 1) {
            return anyConstructor();
        } else {
            return constructorMatchingArguments(field.getAnnotation(Instance.class));
        }
    }

    private static Predicate<Constructor<?>> anyConstructor() {
        return it -> true;
    }

    private static Predicate<Constructor<?>> constructorMatchingArguments(Instance annotation) {
        return it -> Arrays.equals(it.getParameterTypes(), annotation.parameterTypes());
    }

    Object resolve(Parameter parameter) {
        var type = parameter.getType();
        var name = parameter.getName();

        // Either a type, name combination is defined
        if (isDefined(type, name)) {
            return lookup(type, name);
        }

        // Or there exists a unique instance for the type
        if (isUniquelyDefined(type)) {
            return lookupUnique(type);
        }

        // Otherwise
        if (typeToNamedInstances.containsKey(type) && typeToNamedInstances.get(type).values().size() > 1) {
            throw new RuntimeException("No unique candidate for %s available are %s".formatted(
                asString(parameter),
                typeToNamedInstances.get(type).keySet()));
        } else {
            throw new RuntimeException("No injection candidate for %s".formatted(asString(parameter)));
        }
    }

    private static Object wrapNullAsDefinedNull(Object instance) {
        return instance == null ? DEFINED_NULL : instance;
    }

    private static Object unwrapDefinedNullToNull(Object instance) {
        return instance == DEFINED_NULL ? null : instance;
    }
}

class ReflectionUtils {
    private ReflectionUtils() {}

    static Object extract(Object instance, Field field) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to extract the value of %s".formatted(asString(field)));
        }
    }

    static void inject(Object instance, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to inject the value %s for %s".formatted(value, asString(field)));
        }
    }

    static String asString(Field field) {
        return "Field[%s %s]".formatted(field.getType().getSimpleName(), field.getName());
    }

    static String asString(Parameter parameter) {
        return "Parameter[%s %s]".formatted(parameter.getType().getSimpleName(), parameter.getName());
    }
}