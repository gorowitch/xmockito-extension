package poc.comparison.domain;

public class CustomerRegistrationService {
    private final MailSender mailSender;
    private final EmailAddressValidator emailValidator;
    private final MailComposer mailComposer;
    private final CustomerRepository customerRepository;

    public CustomerRegistrationService(
        MailSender mailSender,
        EmailAddressValidator emailValidator,
        MailComposer mailComposer,
        CustomerRepository customerRepository
    ) {
        this.mailSender = mailSender;
        this.emailValidator = emailValidator;
        this.mailComposer = mailComposer;
        this.customerRepository = customerRepository;
    }

    public Customer register(String firstName, String lastName, String emailAddress) {
        emailValidator.validate(emailAddress);

        if(customerRepository.findByEmailAddress(emailAddress).isPresent()) {
            throw new ValidationException("Email address already registered: %s".formatted(emailAddress));
        }

        Customer customer = customerRepository.save(new Customer(firstName, lastName, emailAddress));

        mailSender.send(mailComposer.generateWelcomeMail(customer));

        return customer;
    }


}
