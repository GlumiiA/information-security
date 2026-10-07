package ru.itmo.secureapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.secureapi.user.UserRepository;

@SpringBootTest
class SecurityIntegrationTest {

    private static final String PASSWORD = "S3cure-Passw0rd";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    private MockMvc mvc;
    private String username;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        register(username, PASSWORD);
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        String hash = users.findByUsername(username).orElseThrow().getPasswordHash();
        assertTrue(hash.startsWith("$2a$12$"), "password must be stored as bcrypt hash");
        assertTrue(!hash.contains(PASSWORD));
    }

    @Test
    void loginReturnsJwt() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(username, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void sqlInjectionInLoginDoesNotBypassAuthentication() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("' OR '1'='1' --", "' OR '1'='1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/data"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void protectedEndpointWithInvalidTokenReturns401() throws Exception {
        String forged = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJhZG1pbiJ9.";
        mvc.perform(get("/api/data").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidTokenReturns200() throws Exception {
        mvc.perform(get("/api/data").header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk());
    }

    @Test
    void userContentIsHtmlEscapedInResponse() throws Exception {
        String token = login();
        String payload = """
                {"title":"<script>alert(1)</script>","content":"<img src=x onerror=alert('xss')>"}
                """;
        mvc.perform(post("/api/data").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("&lt;script&gt;alert(1)&lt;/script&gt;"))
                .andExpect(jsonPath("$.content").value(not(containsString("<img"))));

        mvc.perform(get("/api/data").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    void sqlInjectionInSearchIsTreatedAsPlainText() throws Exception {
        mvc.perform(get("/api/data").param("q", "' OR 1=1; DROP TABLE posts; --")
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void accountIsTemporarilyLockedAfterTooManyFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(username, "bad-" + i)))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(username, PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void weakRegistrationInputIsRejected() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("<b>x</b>", "short")))
                .andExpect(status().isBadRequest());
    }

    private void register(String user, String password) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials(user, password)))
                .andExpect(status().isCreated());
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String credentials(String user, String password) {
        return "{\"username\":\"" + user.replace("\"", "\\\"") + "\",\"password\":\"" + password.replace("\"", "\\\"") + "\"}";
    }
}
