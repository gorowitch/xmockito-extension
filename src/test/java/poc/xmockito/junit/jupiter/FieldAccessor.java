package poc.xmockito.junit.jupiter;

import java.lang.reflect.Field;

public class FieldAccessor {
    protected final Field declaredField(String fieldName) {
        try {
            return this.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("No field '%s' declared for class '%s'".formatted(fieldName,this.getClass().getSimpleName()));
        }
    }
}
