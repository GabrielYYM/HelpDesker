package com.corp.DeskHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.mongo.MongoSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.corp.DeskHelper.controller.DashboardController;
import com.corp.DeskHelper.controller.UserController;
import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.enums.UserRole;
import com.corp.DeskHelper.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "cors.allowed-origins=http://localhost:5173",
    "spring.mongodb.uri=mongodb://localhost:27017/deskhelper",
    "spring.mongodb.database=deskhelper",
    "spring.data.mongodb.auto-index-creation=false"
})
class GlobalExceptionHandlerTests {

    @LocalServerPort
    private int port;

    @MockitoBean(name = "mongoSessionRepository")
    private MongoIndexedSessionRepository sessionRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MongoConverter mongoConverter;

    @Autowired
    private UserController userController;

    @Autowired
    private DashboardController dashboardController;

    @Test
    void firstAccessLoginAndLogoutUseTheRealHttpSessionAndCsrf() throws Exception {
        String temporaryHash = passwordEncoder.encode("senha-temporaria");
        User user = User.builder().id("id-teste").code("0042").name("Pessoa de teste").password(temporaryHash).build();
        when(userRepository.findByCode("0042")).thenReturn(Optional.of(user));
        when(userRepository.completeFirstAccess(eq("id-teste"), eq(temporaryHash), anyString())).thenAnswer(invocation -> {
            user.setPassword(invocation.getArgument(2));
            user.setFirstAccess(false);
            return 1L;
        });

        Map<String, MongoSession> sessions = new ConcurrentHashMap<>();
        when(sessionRepository.createSession()).thenAnswer(invocation -> new MongoSession());
        when(sessionRepository.findById(anyString())).thenAnswer(invocation -> sessions.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            MongoSession session = invocation.getArgument(0);
            sessions.put(session.getId(), session);
            return null;
        }).when(sessionRepository).save(any(MongoSession.class));
        doAnswer(invocation -> {
            sessions.remove(invocation.getArgument(0));
            return null;
        }).when(sessionRepository).deleteById(anyString());

        try (HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build()) {
            assertThat(get(client, "/api/me").statusCode()).isEqualTo(401);
            String firstAccess = """
                {"newPassword":"senha-ficticia","confirmPassword":"senha-ficticia"}
                """;
            assertThat(post(client, "/api/auth/first-access", firstAccess, null).statusCode()).isEqualTo(403);
            assertThat(post(client, "/api/auth/first-access", firstAccess, csrf(client)).statusCode()).isEqualTo(401);
            assertThat(user.isFirstAccess()).isTrue();

            var temporaryLogin = post(client, "/api/auth/login", """
                {"code":"0042","password":"senha-temporaria"}
                """, csrf(client));
            assertThat(temporaryLogin.statusCode()).isEqualTo(200);
            assertThat(new ObjectMapper().readTree(temporaryLogin.body()).path("firstAccess").asBoolean()).isTrue();
            assertThat(get(client, "/api/me").statusCode()).isEqualTo(200);
            assertThat(get(client, "/api/chamados").statusCode()).isEqualTo(403);
            assertThat(get(client, "/api/dashboard").statusCode()).isEqualTo(403);
            assertThat(post(client, "/api/auth/first-access", """
                {"newPassword":"senha-temporaria","confirmPassword":"senha-temporaria"}
                """, csrf(client)).statusCode()).isEqualTo(400);
            String temporarySessionId = sessions.values().stream()
                .filter(session -> session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) != null)
                .findFirst().orElseThrow().getId();
            var changed = post(client, "/api/auth/first-access", firstAccess, csrf(client));
            assertThat(changed.statusCode()).isEqualTo(200);
            JsonNode profile = new ObjectMapper().readTree(changed.body());
            assertThat(profile.path("firstAccess").asBoolean()).isFalse();
            assertThat(profile.has("password")).isFalse();
            assertThat(sessions).doesNotContainKey(temporarySessionId);
            assertThat(user.isFirstAccess()).isFalse();
            assertThat(passwordEncoder.matches("senha-ficticia", user.getPassword())).isTrue();

            assertThat(post(client, "/api/auth/login", """
                {"code":"0042","password":"senha-temporaria"}
                """, csrf(client)).statusCode()).isEqualTo(401);
            var me = get(client, "/api/me");
            assertThat(me.statusCode()).isEqualTo(200);
            assertThat(new ObjectMapper().readTree(me.body()).path("code").asText()).isEqualTo("0042");
            assertThat(get(client, "/api/dashboard").statusCode()).isEqualTo(403);
            assertThat(get(client, "/api/users").statusCode()).isEqualTo(403);

            var invalidTicket = post(client, "/api/chamados", """
                {"titulo":" ","descricao":""}
                """, csrf(client));
            assertThat(invalidTicket.statusCode()).isEqualTo(400);
            JsonNode errors = new ObjectMapper().readTree(invalidTicket.body()).path("fields");
            assertThat(errors.has("titulo")).isTrue();
            assertThat(errors.has("descricao")).isTrue();

            assertThat(post(client, "/api/logout", "", csrf(client)).statusCode()).isEqualTo(204);
            assertThat(get(client, "/api/me").statusCode()).isEqualTo(401);
            assertThat(sessions.values()).noneMatch(session ->
                session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) != null);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode csrf(HttpClient client) throws Exception {
        var response = get(client, "/api/auth/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        return new ObjectMapper().readTree(response.body());
    }

    private HttpResponse<String> post(HttpClient client, String path, String body, JsonNode csrf) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (csrf != null) {
            request.header(csrf.path("headerName").asText(), csrf.path("token").asText());
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

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
