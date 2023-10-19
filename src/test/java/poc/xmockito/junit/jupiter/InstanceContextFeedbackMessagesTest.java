package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InstanceContextFeedbackMessagesTest {

    private String someValue;
    private String anotherValue;

    @Instance()
    private SinglePublicConstructorInstance instantiatable;

    @Instance()
    private SinglePrivateConstructorInstance nonInstantiatable;

    @Instance(parameterTypes = {Void.class})
    private MultiplePublicConstructorInstance noneAreMatching;

    final InstanceContext context = new InstanceContext();

    @Test
    void feedbackMessage_noInjectionCandidateFound() {
        Field subjectField = declaredField("instantiatable");
        assertThatThrownBy(() -> context.instantiate(subjectField))
            .hasMessage("No injection candidate for Parameter[String value] of constructor SinglePublicConstructorInstance(String value)");
    }

    @Test
    void feedbackMessage_noUniqueInjectionCandidateFound() {
        context.registerInstance(declaredField("someValue"), "");
        context.registerInstance(declaredField("anotherValue"), "");

        Field subjectField = declaredField("instantiatable");
        assertThatThrownBy(() -> context.instantiate(subjectField))
            .hasMessage("%s%s%s".formatted(
                "No unique candidate for Parameter[String value] of constructor SinglePublicConstructorInstance(String value)",
                System.lineSeparator(),
                "available are [someValue, anotherValue]"
            ));
    }

    @Test
    void feedbackMessage_noPublicConstructorFoundForClass() {
        Field subjectField = declaredField("nonInstantiatable");
        assertThatThrownBy(() -> context.instantiate(subjectField))
            .hasMessage("No public constructor to initialize Field[SinglePrivateConstructorInstance nonInstantiatable]");
    }

    @Test
    void feedbackMessage_noMatchingConstructorFoundForClass() {
        Field subjectField = declaredField("noneAreMatching");
        assertThatThrownBy(() -> context.instantiate(subjectField))
            .hasMessage("No matching constructor to initialize Field[MultiplePublicConstructorInstance noneAreMatching]");
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

    private Field declaredField(String fieldName) {
        try {
            return this.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Expecting a field %s".formatted(fieldName));
        }
    }
}
