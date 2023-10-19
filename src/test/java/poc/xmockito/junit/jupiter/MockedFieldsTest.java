package poc.xmockito.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

@ExtendWith(XMockitoExtension.class)
public class MockedFieldsTest {
    @Mock
    private Dependency alpha;

    @Mock
    private Dependency beta;

    @Test
    public void mockedFieldsAreInitializedWithMocks() {
        assertThat(mockingDetails(alpha).isMock()).isTrue();
        assertThat(mockingDetails(beta).isMock()).isTrue();
    }

    @Test
    public void mockedFieldsHoldDifferentInstances() {
        assertThat(alpha).isNotSameAs(beta);
    }
}


