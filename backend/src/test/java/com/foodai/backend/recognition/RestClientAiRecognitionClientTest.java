package com.foodai.backend.recognition;

import com.foodai.backend.config.AiServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class RestClientAiRecognitionClientTest {

    private static final String BASE_URL = "http://ai.example.test";
    private static final String RECOGNITION_PATH = "/v1/recognize";

    @Test
    void forwardsOpaqueJsonResponseWithoutAssumingItsSchema() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(BASE_URL + RECOGNITION_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", startsWith("multipart/form-data")))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"payload\":[1,2]}",
                        MediaType.APPLICATION_JSON));

        var result = fixture.client.recognize(new byte[]{1, 2, 3}, MediaType.IMAGE_JPEG);

        assertEquals(2, result.get("payload").size());
        fixture.server.verify();
    }

    @Test
    void preservesAnEmptyJsonArrayWithoutInventingAResult() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(BASE_URL + RECOGNITION_PATH))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));

        var result = fixture.client.recognize(new byte[]{1}, MediaType.IMAGE_PNG);

        assertEquals(true, result.isArray());
        assertEquals(0, result.size());
        fixture.server.verify();
    }

    @Test
    void mapsEmptyHttpBodyToInvalidUpstreamResponse() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(BASE_URL + RECOGNITION_PATH))
                .andRespond(MockRestResponseCreators.withSuccess("", MediaType.APPLICATION_JSON));

        AiRecognitionException exception = assertThrows(
                AiRecognitionException.class,
                () -> fixture.client.recognize(new byte[]{1}, MediaType.IMAGE_JPEG));

        assertEquals("AI_SERVICE_INVALID_RESPONSE", exception.getCode());
        assertEquals(502, exception.getStatus().value());
        fixture.server.verify();
    }

    @Test
    void mapsHttpServerFailureToBadGateway() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(BASE_URL + RECOGNITION_PATH))
                .andRespond(MockRestResponseCreators.withServerError());

        AiRecognitionException exception = assertThrows(
                AiRecognitionException.class,
                () -> fixture.client.recognize(new byte[]{1}, MediaType.IMAGE_JPEG));

        assertEquals("AI_SERVICE_ERROR", exception.getCode());
        assertEquals(502, exception.getStatus().value());
        fixture.server.verify();
    }

    @Test
    void mapsMalformedJsonToInvalidUpstreamResponse() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(BASE_URL + RECOGNITION_PATH))
                .andRespond(MockRestResponseCreators.withSuccess("{", MediaType.APPLICATION_JSON));

        AiRecognitionException exception = assertThrows(
                AiRecognitionException.class,
                () -> fixture.client.recognize(new byte[]{1}, MediaType.IMAGE_JPEG));

        assertEquals("AI_SERVICE_INVALID_RESPONSE", exception.getCode());
        assertEquals(502, exception.getStatus().value());
        fixture.server.verify();
    }

    @Test
    void mapsTimeoutToGatewayTimeout() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(BASE_URL + RECOGNITION_PATH))
                .andRespond(MockRestResponseCreators.withException(new SocketTimeoutException("test timeout")));

        AiRecognitionException exception = assertThrows(
                AiRecognitionException.class,
                () -> fixture.client.recognize(new byte[]{1}, MediaType.IMAGE_JPEG));

        assertEquals("AI_SERVICE_TIMEOUT", exception.getCode());
        assertEquals(504, exception.getStatus().value());
        fixture.server.verify();
    }

    @Test
    void mapsConnectionFailureToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(BASE_URL + RECOGNITION_PATH))
                .andRespond(MockRestResponseCreators.withException(new ConnectException("test connection failure")));

        AiRecognitionException exception = assertThrows(
                AiRecognitionException.class,
                () -> fixture.client.recognize(new byte[]{1}, MediaType.IMAGE_JPEG));

        assertEquals("AI_SERVICE_UNAVAILABLE", exception.getCode());
        assertEquals(503, exception.getStatus().value());
        fixture.server.verify();
    }

    @Test
    void incompleteMultipartConfigurationFailsFast() {
        AiServiceProperties properties = new AiServiceProperties(
                BASE_URL, "", "", "", Duration.ofSeconds(2), Duration.ofSeconds(30));
        assertThrows(IllegalStateException.class, () -> new RestClientAiRecognitionClient(properties));
    }

    private static Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServiceProperties properties = new AiServiceProperties(
                BASE_URL,
                RECOGNITION_PATH,
                "MULTIPART",
                "image",
                Duration.ofSeconds(2),
                Duration.ofSeconds(30));
        return new Fixture(new RestClientAiRecognitionClient(properties, builder.build()), server);
    }

    private record Fixture(RestClientAiRecognitionClient client, MockRestServiceServer server) {}
}
