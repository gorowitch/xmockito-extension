package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(XMockitoExtension.class)
public class InstanceWithSingleDependencyCanBeResolvedTest {

    @Mock
    private Dependency whatever;

    @Instance
    private SingleDependencyInstance subject;

    private String other; // helper fields are left alone

    @Test
    public void singleDependencyIsInjected() {
        assertThat(subject.first).isSameAs(whatever);
    }

    public record SingleDependencyInstance(Dependency first) {}
}










