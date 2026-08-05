package org.byteveda.agenteval.contracts;

/**
 * Exception thrown when contract loading or verification encounters an error.
 */
public class ContractException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ContractException(String message) {
        super(message);
    }

    public ContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
