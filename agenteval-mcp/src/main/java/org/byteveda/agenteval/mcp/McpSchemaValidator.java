package org.byteveda.agenteval.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates tool call arguments against a JSON Schema definition.
 *
 * <p>Performs basic type-level validation of required fields and types.
 * This is a lightweight validator, not a full JSON Schema implementation.</p>
 */
public final class McpSchemaValidator {

    private static final Logger LOG = LoggerFactory.getLogger(McpSchemaValidator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private McpSchemaValidator() {}

    /**
     * Validates tool arguments against a schema.
     *
     * @param arguments the tool call arguments
     * @param schema the JSON schema as a JsonNode
     * @return list of validation errors (empty if valid)
     */
    public static List<String> validate(Map<String, Object> arguments, JsonNode schema) {
        Objects.requireNonNull(arguments, "arguments must not be null");
        Objects.requireNonNull(schema, "schema must not be null");

        List<String> errors = new ArrayList<>();

        // Check required fields
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode reqField : required) {
                String fieldName = reqField.asText();
                if (!arguments.containsKey(fieldName)) {
                    errors.add("Missing required field: " + fieldName);
                }
            }
        }

        // Check property types
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            arguments.forEach((key, value) -> {
                JsonNode propSchema = properties.get(key);
                if (propSchema == null) {
                    if (schema.has("additionalProperties")
                            && !schema.get("additionalProperties").asBoolean(true)) {
                        errors.add("Unknown property: " + key);
                    }
                    return;
                }
                String expectedType = propSchema.has("type")
                        ? propSchema.get("type").asText() : null;
                if (expectedType != null && value != null) {
                    String actualType = jsonType(value);
                    if (!expectedType.equals(actualType)) {
                        errors.add(String.format(
                                "Property '%s' expected type '%s' but got '%s'",
                                key, expectedType, actualType));
                    }
                }
            });
        }

        if (!errors.isEmpty()) {
            LOG.debug("Schema validation failed: {}", errors);
        }
        return errors;
    }

    /**
     * Parses a JSON schema string into a JsonNode.
     */
    public static JsonNode parseSchema(String jsonSchema) {
        try {
            return MAPPER.readTree(jsonSchema);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON schema: " + e.getMessage(), e);
        }
    }

    private static String jsonType(Object value) {
        if (value instanceof String) return "string";
        if (value instanceof Integer || value instanceof Long) return "integer";
        if (value instanceof Number) return "number";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List<?>) return "array";
        if (value instanceof Map<?, ?>) return "object";
        return "unknown";
    }
}
