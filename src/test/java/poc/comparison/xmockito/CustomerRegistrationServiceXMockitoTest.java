package poc.comparison.xmockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import poc.comparison.domain.*;
import poc.xmockito.junit.jupiter.Instance;
import poc.xmockito.junit.jupiter.Mock;
import poc.xmockito.junit.jupiter.XMockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(XMockitoExtension.class)
public class CustomerRegistrationServiceXMockitoTest {

    EmailAddressValidator emailAddressValidator = new EmailAddressValidator();

    MailComposer mailComposer = new MailComposer();

    @Mock
    CustomerRepository repository;
    @Mock
    MailSender mailSender;

    @Instance
    CustomerRegistrationService service;

    @Test
    public void registration_validates_customer_email() {
        assertThatThrownBy(() -> service.register("Avery", "Buylot", "invalid-email-address"))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Invalid email address: invalid-email-address");
    }

    @Test
    public void registration_rejects_emailAddress_that_is_already_registered() {
        when(repository.findByEmailAddress("alreadyregistered@example.com"))
            .thenReturn(Optional.of(new Customer("Al", "Ready", "alreadyregistered@example.com")));

        assertThatThrownBy(() -> service.register("Avery", "Buylot", "alreadyregistered@example.com"))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Email address already registered: alreadyregistered@example.com");
    }

    @Test
    public void registration_creates_new_customer() {
        when(repository.save(any(Customer.class))).thenAnswer((Answer<Customer>) invocation -> invocation.getArgument(0));

        Customer customer = service.register("Avery", "Buylot", "avery1987@example.com");

        assertThat(customer.getFirstName()).isEqualTo("Avery");
        assertThat(customer.getLastName()).isEqualTo("Buylot");
        assertThat(customer.getEmailAddress()).isEqualTo("avery1987@example.com");

        assertThat(customer.getId()).isNotNull();
    }

    @Test
    public void registration_saves_registered_customer() {
        when(repository.save(any(Customer.class))).thenAnswer((Answer<Customer>) invocation -> invocation.getArgument(0));

        service.register("Avery", "Buylot", "avery1987@example.com");

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(captor.capture());
        Customer customer = captor.getValue();

        assertThat(customer.getFirstName()).isEqualTo("Avery");
        assertThat(customer.getLastName()).isEqualTo("Buylot");
        assertThat(customer.getEmailAddress()).isEqualTo("avery1987@example.com");

        assertThat(customer.getId()).isNotNull();
    }

    @Test
    public void registration_sends_welcomeMail() {
        when(repository.save(any(Customer.class))).thenAnswer((Answer<Customer>) invocation -> invocation.getArgument(0));

        service.register("Avery", "Buylot", "avery1987@example.com");

        ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getFrom()).isEqualTo("welcome@example.com");
        assertThat(captor.getValue().getTo()).isEqualTo("avery1987@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Welcome Avery Buylot");
        assertThat(captor.getValue().getBody()).isEqualTo(
            """
                Hi Avery,
                    
                Welcome to 
                    
                ...
                """
        );
    }
}

