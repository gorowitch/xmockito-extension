package poc.xmockito.junit.jupiter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

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
