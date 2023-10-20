package com.interlogic.app.exceptions;

public class InterlogicException extends RuntimeException {

    public InterlogicException(Error message, Throwable cause) {
        super(message.name(), cause);
    }

    public InterlogicException(Error message) {
        super(message.name());
    }
}
