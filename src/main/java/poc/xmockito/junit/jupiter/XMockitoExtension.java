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

