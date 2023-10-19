package poc.xmockito.example.domain;

import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByEmailAddress(String emailAddress);

    Customer save(Customer entity);
}
