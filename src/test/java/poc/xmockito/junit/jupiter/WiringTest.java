package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WiringTest {

    @Nested
    class InstancesAreWiredBottomUp extends FieldAccessor {

        public record A(B b) {
        }

        public record B() {
        }

        private A a;
        private B b;

        final WiringEngine context = new WiringEngine();

        @Test
        public void wiringDealsWithInstantiationOrder() {
            Field a = declaredField("a");
            Field b = declaredField("b");

            context.wireInstances(List.of(a, b));

            assertThat(context.lookup(a.getType(), a.getName())).isNotNull();
            assertThat(context.lookup(b.getType(), b.getName())).isNotNull();
        }
    }

    @Nested
    class CircularReferencesAreDetected extends FieldAccessor {

        public record Q(P p) {
        }

        public record P(Q q) {
        }

        public record R(R r1, R r2) {
        }

        private Q q;
        private P p;
        private R r;

        final WiringEngine context = new WiringEngine();

        @Test
        public void feedbackMessage_includesAllConstructorsWithParameterWithoutAnInjectionCandidate() {
            Field q = declaredField("q");
            Field p = declaredField("p");

            assertThatThrownBy(() -> context.wireInstances(List.of(q, p)))
                .isInstanceOf(WiringException.class)
                .hasMessageContainingAll(
                    "Field[Q q] -> new Q(P p)",
                "No injection candidate for Parameter[P p]",
                "No injection candidate for Parameter[Q q]"
            );
        }

        @Test
        public void feedbackMessage_includesAllParametersWithoutAnInjectionCandidate() {
            Field r = declaredField("r");
            assertThatThrownBy(() -> context.wireInstances(List.of(r)))
                .isInstanceOf(WiringException.class)
                .hasMessageContainingAll(
                    "Field[R r] -> new R(R r1, R r2)",
                    "No injection candidate for Parameter[R r1]",
                    "No injection candidate for Parameter[R r2]"
                );
        }
    }
}
