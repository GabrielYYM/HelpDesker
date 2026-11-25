package com.corp.DeskHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.corp.DeskHelper.chamado.DashboardController;
import com.corp.DeskHelper.controller.UserController;
import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.enums.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "cors.allowed-origins=http://localhost:5173",
    "spring.mongodb.uri=mongodb://localhost:27017/deskhelper",
    "spring.mongodb.database=deskhelper"
})
class GlobalExceptionHandlerTests {

    @LocalServerPort
    private int port;

    @MockitoBean(name = "mongoSessionRepository")
    private MongoIndexedSessionRepository sessionRepository;

    @Autowired
    private MongoConverter mongoConverter;

    @Autowired
    private UserController userController;

    @Autowired
    private DashboardController dashboardController;

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotReadDashboard() {
        assertThatThrownBy(dashboardController::consultar).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "IT")
    void itCannotCreateAnAdministrator() {
        assertThatThrownBy(() -> userController.createUser(new UserCreateRequestDTO("Teste", UserRole.ADM)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void frontendCanObtainCsrfTokenAndLogOut() throws Exception {
        URI base = URI.create("http://localhost:" + port);
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest csrfRequest = HttpRequest.newBuilder(base.resolve("/api/auth/csrf")).build();
            HttpResponse<String> csrf = client.send(csrfRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(csrf.statusCode()).isEqualTo(200);
            assertThat(csrf.headers().firstValue("Cache-Control")).contains("no-store");

            JsonNode token = new ObjectMapper().readTree(csrf.body());
            String cookie = csrf.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
            assertThat(cookie).startsWith("XSRF-TOKEN=");
            assertThat(token.path("token").asText()).isNotBlank();

            HttpRequest logout = HttpRequest.newBuilder(base.resolve("/api/logout"))
                .header("Cookie", cookie)
                .header(token.path("headerName").asText(), token.path("token").asText())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<Void> response = client.send(logout, HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isEqualTo(204);
        }
    }

    @Test
    void mongoReadsLegacyRolesAndWritesCurrentNames() {
        Map.of("ADMINISTRADOR", UserRole.ADM, "USUARIO", UserRole.USER, "GERENTE", UserRole.IT)
            .forEach((legacyName, role) -> {
                User user = mongoConverter.read(User.class, new Document("role", legacyName));
                assertThat(user.getRole()).isEqualTo(role);

                Document saved = new Document();
                mongoConverter.write(user, saved);
                assertThat(saved.getString("role")).isEqualTo(role.name());
                assertThat(mongoConverter.read(User.class, saved).getRole()).isEqualTo(role);
            });
    }

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
