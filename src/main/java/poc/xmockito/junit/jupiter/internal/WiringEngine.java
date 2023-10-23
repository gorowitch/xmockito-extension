package poc.xmockito.junit.jupiter.internal;

import poc.xmockito.junit.jupiter.Instance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static poc.xmockito.junit.jupiter.internal.MultipleParametersResult.combine;
import static poc.xmockito.junit.jupiter.internal.SingleParameterResolution.resolved;
import static poc.xmockito.junit.jupiter.internal.SingleParameterResolution.unresolved;

public class WiringEngine {
    private final WiringContext context = new WiringContext();

    public void clear() {
        context.clear();
    }

    public void register(Field predefined, Object extract) {
        context.register(predefined, extract);
    }

    public Object lookup(Class<?> type, String name) {
        return context.lookup(type, name);
    }

    public void wireInstances(List<Field> fields) {
        LinkedList<Field> fieldsToInstantiate = new LinkedList<>(fields);

        int size;
        do {
            size = fieldsToInstantiate.size();
            for (Iterator<Field> iterator = fieldsToInstantiate.iterator(); iterator.hasNext(); ) {
                Field field = iterator.next();

                InstantiationResult instantiate = this.instantiate(field);
                if (instantiate instanceof InstanceCreated created) {
                    this.register(field, created.instance());
                    iterator.remove();
                }

            }
        } while (size > fieldsToInstantiate.size());

        if (fieldsToInstantiate.size() > 0) {
            throw new WiringException(
                fieldsToInstantiate.stream()
                    .map(this::instantiate)
                    .filter(result -> result instanceof InstanceCreationFailed)
                    .map(result -> (InstanceCreationFailed) result)
                    .map(InstanceCreationFailed::message).
                    collect(Collectors.joining(System.lineSeparator()))
            );
        }
    }

    InstantiationResult instantiate(Field field) {
        ConstructorResult constructorResolution = selectConstructor(field);

        if (constructorResolution instanceof ConstructorSelected resolved) {
            Constructor<?> selectedConstructor = resolved.constructor();
            MultipleParametersResult resolution = resolvedParameters(selectedConstructor);

            if (resolution instanceof AllParametersResolved allResolved) {
                try {
                    return new InstanceCreated(selectedConstructor.newInstance(allResolved.parameters()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new WiringException("Unable to instantiate %s".formatted(ReflectionUtils.asString(field)), e);
                }
            }
            if (resolution instanceof SomeParametersUnresolved someUnresolved) {
                return new InstanceCreationFailed(field, resolved, someUnresolved);
            }
            throw new IllegalStateException();
        }
        if (constructorResolution instanceof ConstructorNotFound unresolved) {
            return new InstanceCreationFailed(field, unresolved);
        }
        throw new IllegalStateException();
    }

    private MultipleParametersResult resolvedParameters(Constructor<?> selectedConstructor) {
        return combine(stream(selectedConstructor.getParameters()).map(this::resolve).toList());
    }

    private static ConstructorResult selectConstructor(Field dependency) {
        var constructors = dependency.getType().getConstructors();
        if (constructors.length == 0) {
            return new ConstructorNotFound();
        }

        return stream(constructors).filter(constructorSelector(constructors, dependency)).map(it -> (ConstructorResult) new ConstructorSelected(it)).findFirst()
            .orElseGet(() -> new ConstructorNotFound(List.of(constructors)));
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

    public SingleParameterResolution resolve(Parameter parameter) {
        var type = parameter.getType();
        var name = parameter.getName();

        WiringContext.ParameterDefinition definition;

        definition = context.parameterDefinition(type, name);

        return switch (definition) {
            case UNIQUE_BY_TYPE_AND_NAME -> resolved(lookup(type, name));
            case UNIQUE_BY_TYPE -> resolved(context.lookup(type));
            case NONUNIQUE_BY_TYPE -> {
                yield unresolved(
                    "No unique candidate for %s%s\t\tavailable candidates are %s"
                        .formatted(
                            ReflectionUtils.asString(parameter),
                            System.lineSeparator(),
                            context.lookupNamesFor(type)));
            }
            case UNDEFINED -> {
                yield unresolved(
                    "No injection candidate for %s"
                        .formatted(ReflectionUtils.asString(parameter)));
            }
        };
    }
}
