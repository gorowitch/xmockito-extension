package poc.comparison.domain;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class EmailAddressValidator {
    private final Predicate<String> IS_EMAIL = Pattern.compile("^(.+)@(.+)$").asMatchPredicate();

    void validate(String emailAddress) {
        if (!IS_EMAIL.test(emailAddress)) {
            throw new ValidationException("Invalid email address: %s".formatted(emailAddress));
        }
    }
}
