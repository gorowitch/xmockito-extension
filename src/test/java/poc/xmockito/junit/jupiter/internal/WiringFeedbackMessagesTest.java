package poc.xmockito.junit.jupiter.internal;

import org.junit.jupiter.api.Test;
import poc.xmockito.junit.jupiter.FieldAccessor;
import poc.xmockito.junit.jupiter.Instance;
import poc.xmockito.junit.jupiter.internal.InstanceCreationFailed;
import poc.xmockito.junit.jupiter.internal.InstantiationResult;
import poc.xmockito.junit.jupiter.internal.WiringEngine;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class WiringFeedbackMessagesTest extends FieldAccessor {

    private String someValue;
    private String anotherValue;

    @Instance()
    private SinglePublicConstructorInstance instantiatable;

    @Instance()
    private SinglePrivateConstructorInstance nonInstantiatable;

    @Instance(parameterTypes = {Void.class})
    private MultiplePublicConstructorInstance noneAreMatching;

    final WiringEngine context = new WiringEngine();

    @Test
    void feedbackMessage_noInjectionCandidateFound() {
        Field subjectField = declaredField("instantiatable");

        InstantiationResult result = context.instantiate(subjectField);
        assertThat(result).isInstanceOf(InstanceCreationFailed.class);
        assertThat(((InstanceCreationFailed) result).message())
            .isEqualTo(lines(
                "Field[SinglePublicConstructorInstance instantiatable] -> new SinglePublicConstructorInstance(String value)",
                "\tNo injection candidate for Parameter[String value]"
            ));
    }

    @Test
    void feedbackMessage_noUniqueInjectionCandidateFound() {
        context.register(declaredField("someValue"), "");
        context.register(declaredField("anotherValue"), "");

        Field subjectField = declaredField("instantiatable");

        InstantiationResult result = context.instantiate(subjectField);
        assertThat(result).isInstanceOf(InstanceCreationFailed.class);
        assertThat(((InstanceCreationFailed) result).message())
            .isEqualTo(lines(
                "Field[SinglePublicConstructorInstance instantiatable] -> new SinglePublicConstructorInstance(String value)",
                "\tNo unique candidate for Parameter[String value]",
                "\t\tavailable candidates are [someValue, anotherValue]"
            ));
    }

    @Test
    void feedbackMessage_noPublicConstructorFoundForClass() {
        Field subjectField = declaredField("nonInstantiatable");

        InstantiationResult result = context.instantiate(subjectField);
        assertThat(result).isInstanceOf(InstanceCreationFailed.class);
        assertThat(((InstanceCreationFailed) result).message())
            .isEqualTo(lines(
                "Field[SinglePrivateConstructorInstance nonInstantiatable] -> No public constructor found"
            ));
    }

    @Test
    void feedbackMessage_noMatchingConstructorFoundForClass() {
        Field subjectField = declaredField("noneAreMatching");

        InstantiationResult result = context.instantiate(subjectField);
        assertThat(result).isInstanceOf(InstanceCreationFailed.class);
        assertThat(((InstanceCreationFailed) result).message())
            .isEqualTo(lines(
                "Field[MultiplePublicConstructorInstance noneAreMatching] -> No matching constructor found",
                "\tavailable candidates are:",
                "\t\tMultiplePublicConstructorInstance(Integer arg0)",
                "\t\tMultiplePublicConstructorInstance(String arg0)"
            ));
    }

    public record SinglePublicConstructorInstance(String value) {
    }

    private static class SinglePrivateConstructorInstance {
        private SinglePrivateConstructorInstance(String value) {
        }
    }

    private static class MultiplePublicConstructorInstance {
        public MultiplePublicConstructorInstance(Integer value) {
        }

        public MultiplePublicConstructorInstance(String value) {
        }
    }

    private static String lines(String... lines) {
        return Arrays.stream(lines).collect(Collectors.joining(System.lineSeparator()));
    }
}
