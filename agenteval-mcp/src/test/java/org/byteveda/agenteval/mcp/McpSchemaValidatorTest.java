package org.byteveda.agenteval.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpSchemaValidatorTest {

    @Test
    void shouldPassValidArguments() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "query": {"type": "string"},
                    "limit": {"type": "integer"}
                  },
                  "required": ["query"]
                }""";

        JsonNode schemaNode = McpSchemaValidator.parseSchema(schema);
        List<String> errors = McpSchemaValidator.validate(
                Map.of("query", "test", "limit", 10), schemaNode);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldDetectMissingRequiredField() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "query": {"type": "string"}
                  },
                  "required": ["query"]
                }""";

        JsonNode schemaNode = McpSchemaValidator.parseSchema(schema);
        List<String> errors = McpSchemaValidator.validate(Map.of(), schemaNode);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).contains("Missing required field: query");
    }

    @Test
    void shouldDetectTypeMismatch() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "count": {"type": "integer"}
                  }
                }""";

        JsonNode schemaNode = McpSchemaValidator.parseSchema(schema);
        List<String> errors = McpSchemaValidator.validate(
                Map.of("count", "not a number"), schemaNode);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).contains("expected type 'integer'");
    }

    @Test
    void shouldDetectUnknownPropertyWhenNotAllowed() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": {"type": "string"}
                  },
                  "additionalProperties": false
                }""";

        JsonNode schemaNode = McpSchemaValidator.parseSchema(schema);
        List<String> errors = McpSchemaValidator.validate(
                Map.of("name", "test", "extra", "value"), schemaNode);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).contains("Unknown property: extra");
    }

    @Test
    void shouldAllowUnknownPropertyByDefault() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": {"type": "string"}
                  }
                }""";

        JsonNode schemaNode = McpSchemaValidator.parseSchema(schema);
        List<String> errors = McpSchemaValidator.validate(
                Map.of("name", "test", "extra", "value"), schemaNode);

        assertThat(errors).isEmpty();
    }
}
