package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static poc.xmockito.junit.jupiter.ReflectionUtils.*;

public class XMockitoExtension implements BeforeEachCallback, AfterEachCallback {

    private final WiringContext context = new WiringContext();

    public void beforeEach(ExtensionContext context) {
        this.context.clear();
        Object testInstance = context.getTestInstance().get();

        // Collect Predefined
        for (Field predefined : dependenciesToCollect(testInstance)) {
            this.context.register(predefined, extract(testInstance, predefined));
        }

        // Create Mocks
        for (Field mocked : dependenciesToMock(testInstance)) {
            this.context.register(mocked, Mockito.mock(mocked.getType()));
        }

        // Create Instances
        this.context.wireInstances(dependenciesToInstantiate(testInstance));

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

class WiringException extends RuntimeException {
    public WiringException(String message) {
        super(message);
    }

    public WiringException(String message, Throwable cause) {
        super(message, cause);
    }
}

class WiringContext {
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

    boolean canInstantiate(Field field) {
        return canResolveParameters(selectConstructor(field));
    }

    Object instantiate(Field field) {
        try {
            Constructor<?> selectedConstructor = selectConstructor(field);
            Object[] parameters = resolvedParameters(selectedConstructor);
            return selectedConstructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new WiringException("Unable to instantiate %s".formatted(asString(field)), e);
        }
    }

    private boolean canResolveParameters(Constructor<?> selectedConstructor) {
        return stream(selectedConstructor.getParameters()).allMatch(this::canResolve);
    }

    private Object[] resolvedParameters(Constructor<?> selectedConstructor) {
        return stream(selectedConstructor.getParameters())
            .map(parameter -> resolve(parameter,selectedConstructor))
            .toArray();
    }

    private static Constructor<?> selectConstructor(Field dependency) {
        return selectCandidateConstructor(dependency)
            .orElseThrow(() -> new WiringException("No matching constructor to initialize %s".formatted(asString(dependency))));
    }

    private static Optional<Constructor<?>> selectCandidateConstructor(Field dependency) {
        var constructors = dependency.getType().getConstructors();
        if (constructors.length == 0) {
            throw new WiringException("No public constructor to initialize %s".formatted(asString(dependency)));
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

    boolean canResolve(Parameter parameter) {
        var type = parameter.getType();
        var name = parameter.getName();

        return isDefined(type, name) || isUniquelyDefined(type);
    }

    Object resolve(Parameter parameter,Constructor<?> selectedConstructor) {
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
            throw new WiringException("No unique candidate for %s of constructor %s%savailable are %s".formatted(
                asString(parameter),
                asString(selectedConstructor),
                System.lineSeparator(),
                typeToNamedInstances.get(type).keySet()));
        } else {
            throw new WiringException("No injection candidate for %s of constructor %s".formatted(asString(parameter),asString(selectedConstructor)));
        }
    }

    public void wireInstances(List<Field> fields) {
        LinkedList<Field> fieldsToInstantiate = new LinkedList<>(fields);

        int size;
        do {
            size = fieldsToInstantiate.size();
            for (Iterator<Field> iterator = fieldsToInstantiate.iterator(); iterator.hasNext(); ) {
                Field field = iterator.next();
                if (this.canInstantiate(field)) {
                    this.register(field, this.instantiate(field));
                    iterator.remove();
                }
            }
        } while (size > fieldsToInstantiate.size());

        List<String> messages = new ArrayList<>();
        for (Field field : fieldsToInstantiate) {
            try {
                this.instantiate(field);
            } catch (WiringException e) {
                messages.add(e.getMessage());
            }
        }
        if (messages.size() > 0) {
            throw new WiringException(messages.stream().collect(Collectors.joining(System.lineSeparator())));
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

    static String asString(Constructor<?> constructor) {
        return "%s(%s)".formatted(
            constructor.getDeclaringClass().getSimpleName(),
            stream(constructor.getParameters())
                .map(it -> "%s %s".formatted(it.getType().getSimpleName(), it.getName()))
                .collect(Collectors.joining(", "))
        );
    }
}