package poc.xmockito.junit.jupiter.internal;

class WiringException extends RuntimeException {
    public WiringException(String message) {
        super(message);
    }

    public WiringException(String message, Throwable cause) {
        super(message, cause);
    }
}
