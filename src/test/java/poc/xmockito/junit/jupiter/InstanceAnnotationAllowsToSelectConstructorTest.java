package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(XMockitoExtension.class)
public class InstanceAnnotationAllowsToSelectConstructorTest {

    @Mock
    private Set set;
    @Mock
    private List list;

    @Instance(parameterTypes = {Set.class, List.class})
    private MultipleConstructorInstance subjectOne;

    @Instance(parameterTypes = {List.class, Set.class})
    private MultipleConstructorInstance subjectTwo;

    @Test
    public void instancesAreCreatedWithTheRequestedConstructor() {
        assertThat(subjectOne.feedback).isEqualTo("Set set, List list");
        assertThat(subjectTwo.feedback).isEqualTo("List list, Set set");
    }

    private static class MultipleConstructorInstance {
        public final String feedback;

        public MultipleConstructorInstance(Set set, List list) {
            this.feedback = "Set set, List list";
        }

        public MultipleConstructorInstance(List list, Set set) {
            this.feedback = "List list, Set set";
        }
    }
}
