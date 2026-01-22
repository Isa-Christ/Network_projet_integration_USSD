package com.network.projet.ussd.service.external;

import com.network.projet.ussd.domain.enums.ApiResponseStatus;
import com.network.projet.ussd.dto.ExternalApiResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for ApiInvoker service.
 * Tests API call functionality using MockWebServer to simulate external API
 * responses.
 *
 * @author Network USSD Team
 * @version 1.0
 * @since 2026-01-10
 */
class ApiInvokerTest {

    private MockWebServer mock_web_server;
    private ApiInvoker api_invoker;

    /**
     * Sets up the test environment before each test.
     * Initializes MockWebServer and ApiInvoker instance.
     *
     * @throws IOException if server initialization fails
     */
    @BeforeEach
    void setUp() throws IOException {
        mock_web_server = new MockWebServer();
        mock_web_server.start();
        WebClient.Builder web_client_builder = WebClient.builder();
        api_invoker = new ApiInvoker(web_client_builder);
    }

    /**
     * Cleans up test resources after each test.
     *
     * @throws IOException if server shutdown fails
     */
    @AfterEach
    void tearDown() throws IOException {
        mock_web_server.shutdown();
    }

    /**
     * Tests successful GET request execution.
     * Verifies that a 200 OK response is properly handled and mapped.
     */
    @Test
    void shouldReturnSuccessForValidGetRequest() {
        mock_web_server.enqueue(new MockResponse()
                .setBody("{\"message\":\"success\"}")
                .addHeader("Content-Type", "application/json"));

        Mono<ExternalApiResponse> response_mono = api_invoker.makeGetRequest(
                mock_web_server.url("/test").toString(),
                Map.of("Authorization", "Bearer token"));

        StepVerifier.create(response_mono)
                .assertNext(response -> {
                    assertEquals(ApiResponseStatus.SUCCESS, response.getStatus());
                    assertEquals(200, response.getStatusCode());
                    assertEquals("{\"message\":\"success\"}", response.getBody());
                })
                .verifyComplete();
    }

    /**
     * Tests error handling for server errors.
     * Verifies that a 500 Internal Server Error is properly categorized.
     */
    @Test
    void shouldReturnServerErrorForFailedRequest() {
        mock_web_server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        Mono<ExternalApiResponse> response_mono = api_invoker.makeGetRequest(
                mock_web_server.url("/error").toString(),
                null);

        StepVerifier.create(response_mono)
                .assertNext(response -> {
                    assertEquals(ApiResponseStatus.SERVER_ERROR, response.getStatus());
                    assertEquals(500, response.getStatusCode());
                })
                .verifyComplete();
    }
}
