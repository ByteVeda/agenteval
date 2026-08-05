package org.byteveda.agenteval.core.config;

/**
 * Unchecked exception for configuration loading errors.
 */
public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
