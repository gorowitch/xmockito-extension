package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static poc.xmockito.junit.jupiter.ReflectionUtils.extract;
import static poc.xmockito.junit.jupiter.ReflectionUtils.inject;

public class XMockitoExtension implements BeforeEachCallback, AfterEachCallback {

    private final WiringEngine context = new WiringEngine();

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

class ReflectionUtils {
    private ReflectionUtils() {
    }

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