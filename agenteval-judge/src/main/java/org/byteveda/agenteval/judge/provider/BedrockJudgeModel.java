package org.byteveda.agenteval.judge.provider;

import org.byteveda.agenteval.core.model.TokenUsage;
import org.byteveda.agenteval.judge.JudgeException;
import org.byteveda.agenteval.judge.config.JudgeConfig;
import org.byteveda.agenteval.judge.http.HttpJudgeClient;
import org.byteveda.agenteval.judge.http.HttpJudgeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Amazon Bedrock judge model provider.
 *
 * <p>Sends requests to the Bedrock Runtime {@code InvokeModel} API using
 * the Anthropic Messages API format (for Claude models on Bedrock).
 * Authentication uses AWS Signature Version 4.</p>
 *
 * <p>Required environment variables: {@code AWS_ACCESS_KEY_ID},
 * {@code AWS_SECRET_ACCESS_KEY}, and optionally {@code AWS_SESSION_TOKEN}.
 * The region is extracted from the base URL
 * ({@code https://bedrock-runtime.{region}.amazonaws.com}).</p>
 */
public final class BedrockJudgeModel extends AbstractHttpJudgeModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://bedrock-runtime.us-east-1.amazonaws.com";
    private static final String INVOKE_PATH = "/model/%s/invoke";
    private static final String SERVICE = "bedrock";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SYSTEM_PROMPT =
            "You are an evaluation judge. Respond ONLY with a JSON object "
                    + "containing \"score\" (a number between 0.0 and 1.0) "
                    + "and \"reason\" (a brief explanation).";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;
    private final String region;

    public BedrockJudgeModel(JudgeConfig config) {
        this(config,
                resolveEnv("AWS_ACCESS_KEY_ID", "Bedrock"),
                resolveEnv("AWS_SECRET_ACCESS_KEY", "Bedrock"),
                System.getenv("AWS_SESSION_TOKEN"));
    }

    public BedrockJudgeModel(JudgeConfig config,
                             String accessKeyId, String secretAccessKey,
                             String sessionToken) {
        super(config);
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new JudgeException("Bedrock requires AWS_ACCESS_KEY_ID");
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            throw new JudgeException("Bedrock requires AWS_SECRET_ACCESS_KEY");
        }
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
        this.region = extractRegion(config.getBaseUrl());
    }

    BedrockJudgeModel(JudgeConfig config,
                      String accessKeyId, String secretAccessKey,
                      String sessionToken,
                      HttpJudgeClient client) {
        super(config, client);
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
        this.region = extractRegion(config.getBaseUrl());
    }

    static String defaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    protected HttpJudgeRequest buildRequest(String prompt) {
        try {
            var body = MAPPER.createObjectNode();
            body.put("anthropic_version", "bedrock-2023-05-31");
            body.put("max_tokens", 1024);
            body.put("temperature", config.getTemperature());
            body.put("system", SYSTEM_PROMPT);

            var messages = body.putArray("messages");
            var userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            String url = config.getBaseUrl()
                    + String.format(INVOKE_PATH, config.getModel());
            String payload = MAPPER.writeValueAsString(body);

            Map<String, String> headers = signRequest(url, payload);
            return new HttpJudgeRequest(url, headers, payload);
        } catch (JudgeException e) {
            throw e;
        } catch (Exception e) {
            throw new JudgeException("Failed to build Bedrock request", e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode content = root.path("content");
        if (content.isEmpty()) {
            throw new JudgeException("No content in Bedrock response");
        }
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                return block.path("text").asText("");
            }
        }
        throw new JudgeException("No text block in Bedrock response content");
    }

    @Override
    protected TokenUsage extractTokenUsage(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode usage = root.path("usage");
        if (usage.isMissingNode()) {
            return null;
        }
        int input = usage.path("input_tokens").asInt(0);
        int output = usage.path("output_tokens").asInt(0);
        return new TokenUsage(input, output, input + output);
    }

    private Map<String, String> signRequest(String url, String payload) {
        try {
            Instant now = Instant.now();
            String dateStamp = DATE_FORMAT.format(now);
            String amzDate = DATETIME_FORMAT.format(now);

            URI uri = URI.create(url);
            String host = uri.getHost();
            String path = uri.getPath();

            String payloadHash = sha256Hex(payload);

            var signedHeaders = new TreeMap<String, String>();
            signedHeaders.put("host", host);
            signedHeaders.put("x-amz-content-sha256", payloadHash);
            signedHeaders.put("x-amz-date", amzDate);
            if (sessionToken != null && !sessionToken.isBlank()) {
                signedHeaders.put("x-amz-security-token", sessionToken);
            }

            String signedHeaderNames = String.join(";", signedHeaders.keySet());

            StringBuilder canonicalHeaders = new StringBuilder();
            for (var entry : signedHeaders.entrySet()) {
                canonicalHeaders.append(entry.getKey()).append(':')
                        .append(entry.getValue()).append('\n');
            }

            String canonicalRequest = String.join("\n",
                    "POST", path, "", canonicalHeaders.toString(),
                    signedHeaderNames, payloadHash);

            String credentialScope = dateStamp + "/" + region + "/" + SERVICE + "/aws4_request";
            String stringToSign = String.join("\n",
                    ALGORITHM, amzDate, credentialScope,
                    sha256Hex(canonicalRequest));

            byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, SERVICE);
            String signature = hexEncode(hmacSha256(signingKey,
                    stringToSign.getBytes(StandardCharsets.UTF_8)));

            String authorization = ALGORITHM + " Credential=" + accessKeyId + "/"
                    + credentialScope + ", SignedHeaders=" + signedHeaderNames
                    + ", Signature=" + signature;

            var headers = new TreeMap<String, String>();
            headers.put("Authorization", authorization);
            headers.put("x-amz-content-sha256", payloadHash);
            headers.put("x-amz-date", amzDate);
            if (sessionToken != null && !sessionToken.isBlank()) {
                headers.put("x-amz-security-token", sessionToken);
            }
            return Map.copyOf(headers);
        } catch (Exception e) {
            throw new JudgeException("Failed to sign Bedrock request", e);
        }
    }

    static String extractRegion(String baseUrl) {
        // Format: https://bedrock-runtime.{region}.amazonaws.com
        try {
            String host = URI.create(baseUrl).getHost();
            String[] parts = host.split("\\.");
            if (parts.length >= 3) {
                return parts[1];
            }
        } catch (Exception e) {
            // fall through
        }
        return "us-east-1";
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return hexEncode(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] getSignatureKey(String key, String dateStamp,
                                          String regionName, String serviceName)
            throws Exception {
        byte[] kDate = hmacSha256(
                ("AWS4" + key).getBytes(StandardCharsets.UTF_8),
                dateStamp.getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(kDate,
                regionName.getBytes(StandardCharsets.UTF_8));
        byte[] kService = hmacSha256(kRegion,
                serviceName.getBytes(StandardCharsets.UTF_8));
        return hmacSha256(kService,
                "aws4_request".getBytes(StandardCharsets.UTF_8));
    }

    private static String hexEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String resolveEnv(String envVar, String providerName) {
        String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            throw new JudgeException(
                    providerName + " requires " + envVar
                            + " environment variable");
        }
        return value;
    }
}
