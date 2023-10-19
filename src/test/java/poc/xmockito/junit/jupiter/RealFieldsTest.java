package poc.xmockito.junit.jupiter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(XMockitoExtension.class)
public class RealFieldsTest {

    @Nested
    class InitializedFieldValuesAreInjected {

        private String value = "@value-to-be-injected@";
        private String other = "other String values don't bother";

        @Instance
        private SingleValueInstance subject;

        @Test
        public void fieldValueIsInjected() {
            Assertions.assertThat(subject.value).isEqualTo("@value-to-be-injected@");
        }
    }

    @Nested
    class NullFieldValuesAreInjected {

        private String value = null;
        private String other = "other String values don't bother";

        @Instance
        private SingleValueInstance subject;

        @Test
        public void fieldValueIsInjected() {
            Assertions.assertThat(subject.value).isNull();
        }
    }

    public record SingleValueInstance(String value) {
    }
}


