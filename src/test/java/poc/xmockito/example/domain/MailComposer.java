package poc.xmockito.example.domain;

public class MailComposer {
    public MailMessage generateWelcomeMail(Customer customer) {
        String firstName = customer.getFirstName();
        return new MailMessage(
            "welcome@example.com",
            customer.getEmailAddress(),
            "Welcome %s %s".formatted(firstName, customer.getLastName()),
            """
                Hi %s,
                                
                Welcome to 
                                
                ...
                """.formatted(firstName));
    }

}
