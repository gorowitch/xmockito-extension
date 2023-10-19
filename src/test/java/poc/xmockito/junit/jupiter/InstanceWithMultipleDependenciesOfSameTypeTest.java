package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(XMockitoExtension.class)
public class InstanceWithMultipleDependenciesOfSameTypeTest {

    @Nested
    class DependenciesAreResolvedByTypeWhenSomeUniqueDependencyWithNonMatchingFieldNameIsAvailable {
        @Mock
        private Dependency whatever;

        @Instance
        private MultipleDependenciesOfSameTypeInstance subject;

        @Test
        public void bothInstanceFieldsAreInjectedWithSameMockInstance() {
            assertThat(subject.first()).isSameAs(whatever);
            assertThat(subject.second()).isSameAs(whatever);
        }
    }

    @Nested
    class DependenciesAreResolvedByTypeWhenOnlyOneOfTheDependenciesWithMatchingFieldNameIsAvailable {
        @Mock
        private Dependency second;

        @Instance
        private MultipleDependenciesOfSameTypeInstance subject;

        @Test
        public void bothInstanceFieldsAreInjectedWithSameMockInstance() {
            assertThat(subject.first()).isSameAs(second);
            assertThat(subject.second()).isSameAs(second);
        }
    }

    /**
     * In order for this feature to work the source needs to be compiled using the javac -parameter option.
     *
     * Intellij: Settings > Build, Execution, Deployment > Compiler > Java Compiler > Additional command line parameters
     *  -parameters
     */
    @Nested
    class DependenciesAreResolvedByNameWhenCorrectlyNamedDependenciesBothMockedAreAvailable {
        @Mock
        private Dependency first;
        @Mock
        private Dependency second;

        @Instance
        private MultipleDependenciesOfSameTypeInstance subject;

        @Test
        public void bothInstanceFieldsAreInjectedByConstructorArgumentName() {
            assertThat(subject.first()).isSameAs(first);
            assertThat(subject.second()).isSameAs(second);
        }
    }

    @Nested
    class DependenciesAreResolvedByNameWhenCorrectlyNamedDependenciesOneMockedAreAvailable {
        @Mock
        private Dependency first;
        private Dependency second = new Dependency();

        @Instance
        private MultipleDependenciesOfSameTypeInstance subject;

        @Test
        public void bothInstanceFieldsAreInjectedByConstructorArgumentName() {
            assertThat(subject.first()).isSameAs(first);
            assertThat(subject.second()).isSameAs(second);
        }
    }

    public record MultipleDependenciesOfSameTypeInstance(Dependency first, Dependency second) {
    }
}








