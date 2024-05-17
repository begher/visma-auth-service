package begh.vismaauthservice;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Setter
@RequiredArgsConstructor
public class AuthService {

    private final WebClient webClient;
    private final AccessRepository repo;

    //@Value("${auth.redirectUri}")
    private String redirectUri = "https://api.imats.se/visma-auth-service/login/oauth2/code/visma";

    //@Value("${auth.clientId}")
    private String clientId = "imatsab";

    //@Value("${auth.clientSecret}")
    private String clientSecret = "M2wAPQ2Y8Q25YHoLEkoA40B4Cv72f6XZGF7ZYZK91380AY98O07R8D7hVMy1vF8";

    private final String credentials = clientId + ":" + clientSecret;
    private final String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
    private Long lastRefresh = 30000L;

    public void redirectToAuthorization(HttpServletResponse response) throws IOException {
        String url = "https://identity-sandbox.test.vismaonline.com/connect/authorize?" +
                "client_id=imatsab&" +
                "redirect_uri=https://api.imats.se/visma-auth-service/login/oauth2/code/visma&" +
                "scope=ea:api%20ea:sales%20ea:purchase%20ea:accounting%20offline_access&" +
                "response_type=code";
        response.sendRedirect(url);
    }
    public Mono<AuthResponse> handleOAuthRedirect(String code) {
        return webClient.post()
                .uri("https://identity-sandbox.test.vismaonline.com/connect/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=authorization_code&code=" + code + "&redirect_uri=" + redirectUri)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .publishOn(Schedulers.boundedElastic())
                .map(this::handleResponse)
                .doOnSuccess(body -> System.out.println("Response Body: " + body))
                .doOnError(error -> System.out.println("Error occurred: " + error.getMessage()));
    }

    public Mono<AuthResponse> refreshToken() {
        return Mono.fromCallable(() -> repo.findAccessByCompany(clientId).orElseThrow().getRefreshToken())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(refreshToken -> webClient.post()
                        .uri("https://identity-sandbox.test.vismaonline.com/connect/token")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue("grant_type=refresh_token&refresh_token=" + refreshToken)
                        .retrieve()
                        .bodyToMono(AuthResponse.class))
                .map(this::handleResponse)
                .doOnSuccess(body -> System.out.println("Response Body: " + body))
                .doOnError(error -> System.out.println("Error occurred: " + error.getMessage()));
    }

    private AuthResponse handleResponse(AuthResponse authResponse) {
        Access access = repo.findAccessByCompany(clientId).orElseGet(Access::new);
        access.setCompany(clientId);
        access.setAccessToken(authResponse.getAccessToken());
        access.setRefreshToken(authResponse.getRefreshToken());
        access.setLastRenewedAt(LocalDateTime.now());
        repo.save(access);
        lastRefresh = System.currentTimeMillis();
        return authResponse;
    }

    public Mono<AuthResponse> getTokens() {
        if ((System.currentTimeMillis() - lastRefresh) / 1000 < 3000) {
            return Mono.fromCallable(() -> repo.findAccessByCompany(clientId))
                    .flatMap(optionalAccess -> optionalAccess
                            .map(access -> Mono.just(new AuthResponse(access.getAccessToken(), 3600, "Bearer", access.getRefreshToken())))
                            .orElseGet(() -> Mono.error(new RuntimeException("Access details not found."))))
                    .switchIfEmpty(Mono.error(new RuntimeException("Access details not found."))); // Handle case where Optional is empty
        }
        return refreshToken();
    }
}
