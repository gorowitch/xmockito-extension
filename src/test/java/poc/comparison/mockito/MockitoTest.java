package poc.comparison.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class MockitoTest {

    @Mock
    private Dependency alpha;
    @Mock
    private Dependency beta;

    @InjectMocks
    private Instance subject;

    @Test
    public void mockedFieldsHoldDifferentInstances() {
        assertThat(alpha).isNotSameAs(beta);
    }

    @Test
    public void sameMockIsInjectedTwice() {
        assertThat(subject.first()).isSameAs(subject.second);
        assertThat(subject.first()).describedAs("mockito: which mock is injected as first varies").isIn(alpha,beta);
    }

    @Test
    public void missingDependenciesAreInjectedAsNull() {
        assertThat(subject.missing()).describedAs("mockito: injects missing dependencies as null").isNull();
    }

    public static class Dependency {}

    public static class MissingDependency {}

    public record Instance(Dependency first, Dependency second, MissingDependency missing) {
    }


}


