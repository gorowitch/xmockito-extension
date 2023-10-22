package poc.xmockito.junit.jupiter;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;


abstract sealed class InstantiationResult permits InstanceCreated, InstanceCreationFailed {
}

final class InstanceCreated extends InstantiationResult {
    private final Object instance;

    InstanceCreated(Object instance) {
        this.instance = instance;
    }

    public Object instance() {
        return instance;
    }
}

final class InstanceCreationFailed extends InstantiationResult {
    private final String message;

    InstanceCreationFailed(SomeParametersUnresolved someParametersUnresolved) {
        this.message = someParametersUnresolved.message();
    }

    InstanceCreationFailed(ConstructorNotFound cause) {
        this.message = cause.message();
    }

    String message() {
        return message;
    }
}

abstract sealed class ConstructorResult permits ConstructorSelected, ConstructorNotFound {
}

final class ConstructorSelected extends ConstructorResult {
    private final Constructor<?> constructor;

    ConstructorSelected(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Constructor<?> constructor() {
        return this.constructor;
    }

}

final class ConstructorNotFound extends ConstructorResult {
    private final String message;

    ConstructorNotFound(String errorMessage) {
        this.message = errorMessage;
    }

    String message() {
        return message;
    }
}

abstract sealed class MultipleParametersResult permits AllParametersResolved, SomeParametersUnresolved {

    public static MultipleParametersResult combine(List<SingleParameterResolution> parameters) {
        if (parameters.stream().allMatch(singleParameterResolution -> singleParameterResolution instanceof ParameterResolved)) {
            return new AllParametersResolved(
                parameters.stream()
                    .map(resolved -> ((ParameterResolved) resolved))
                    .collect(Collectors.toList()));
        } else {
            return new SomeParametersUnresolved((
                parameters.stream()
                    .filter(resolved -> resolved instanceof ParameterUnresolved)
                    .map(resolved -> ((ParameterUnresolved) resolved))
                    .collect(Collectors.toList())));
        }
    }
}

final class AllParametersResolved extends MultipleParametersResult {
    private final List<ParameterResolved> resolvedParameters;

    AllParametersResolved(List<ParameterResolved> resolvedParameters) {
        this.resolvedParameters = resolvedParameters;
    }

    public Object[] parameters() {
        return this.resolvedParameters.stream().map(resolvedParameter -> resolvedParameter.object).toArray();
    }

}

final class SomeParametersUnresolved extends MultipleParametersResult {
    private final List<ParameterUnresolved> unresolvedParameters;

    SomeParametersUnresolved(List<ParameterUnresolved> resolvedParameters) {
        this.unresolvedParameters = resolvedParameters;
    }

    String message() {
        return unresolvedParameters.stream().map(ParameterUnresolved::message).collect(Collectors.joining(System.lineSeparator()));
    }
}

abstract sealed class SingleParameterResolution permits ParameterResolved, ParameterUnresolved {

    public static SingleParameterResolution resolved(Object instance) {
        return new ParameterResolved(instance);
    }

    public static SingleParameterResolution unresolved(String message) {
        return new ParameterUnresolved(message);
    }
}

final class ParameterResolved extends SingleParameterResolution {
    final Object object;

    ParameterResolved(Object object) {
        this.object = object;
    }
}

final class ParameterUnresolved extends SingleParameterResolution {
    private final String message;

    ParameterUnresolved(String errorMessage) {
        this.message = errorMessage;
    }

    String message() {
        return message;
    }
}
