package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

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
        assertThat(((InstanceCreationFailed)result).message())
            .isEqualTo("No injection candidate for Parameter[String value] of constructor SinglePublicConstructorInstance(String value)");
    }

    @Test
    void feedbackMessage_noUniqueInjectionCandidateFound() {
        context.register(declaredField("someValue"), "");
        context.register(declaredField("anotherValue"), "");

        Field subjectField = declaredField("instantiatable");

        InstantiationResult result = context.instantiate(subjectField);
        assertThat(result).isInstanceOf(InstanceCreationFailed.class);
        assertThat(((InstanceCreationFailed)result).message())
            .isEqualTo("%s%s%s".formatted(
                "No unique candidate for Parameter[String value] of constructor SinglePublicConstructorInstance(String value)",
                System.lineSeparator(),
                "available are [someValue, anotherValue]"
            ));
    }

    @Test
    void feedbackMessage_noPublicConstructorFoundForClass() {
        Field subjectField = declaredField("nonInstantiatable");

        InstantiationResult result = context.instantiate(subjectField);
        assertThat(result).isInstanceOf(InstanceCreationFailed.class);
        assertThat(((InstanceCreationFailed)result).message())
            .isEqualTo("No public constructor to initialize Field[SinglePrivateConstructorInstance nonInstantiatable]");
    }

    @Test
    void feedbackMessage_noMatchingConstructorFoundForClass() {
        Field subjectField = declaredField("noneAreMatching");

        InstantiationResult result = context.instantiate(subjectField);
        assertThat(result).isInstanceOf(InstanceCreationFailed.class);
        assertThat(((InstanceCreationFailed)result).message())
            .isEqualTo("No matching constructor to initialize Field[MultiplePublicConstructorInstance noneAreMatching]");
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
}
