package com.corp.DeskHelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "cors.allowed-origins=http://localhost:5173",
    "spring.data.mongodb.uri=mongodb://localhost:27017/deskhelper",
    "spring.data.mongodb.database=deskhelper"
})
class GlobalExceptionHandlerTests {

    @LocalServerPort
    private int port;

    @MockitoBean(name = "mongoSessionRepository")
    private MongoIndexedSessionRepository sessionRepository;

    @Test
    void missingRouteReturnsJsonWithOriginalPath() throws Exception {
        assertError("/api/auth/rota-inexistente", 404, "Not Found");
    }

    @Test
    void unsupportedMethodPreservesStatusAndAllowHeader() throws Exception {
        HttpResponse<String> response = assertError("/api/auth/login", 405, "Method Not Allowed");

        assertThat(response.headers().firstValue("Allow")).contains("POST");
    }

    private HttpResponse<String> assertError(String path, int status, String error) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", "text/html")
            .build();
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        JsonNode body = new ObjectMapper().readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("error").asText()).isEqualTo(error);
        assertThat(body.path("path").asText()).isEqualTo(path);
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(body.hasNonNull("timestamp")).isTrue();
        return response;
    }
}
