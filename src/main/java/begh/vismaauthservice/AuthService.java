package begh.vismaauthservice;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class AuthService {

    private final WebClient webClient;
    private final AccessRepository repo;

    @Value("${auth.redirectUri}")
    private String redirectUri;

    @Value("${auth.clientId}")
    private String clientId;

    @Value("${auth.clientSecret}")
    private String clientSecret;
    private final String credentials = clientId + ":" + clientSecret;
    private final String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());


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

    private AuthResponse handleResponse(AuthResponse authResponse){
        Access access = repo.findAccessByCompany(clientId).orElseGet(() -> Access.builder()
                .company(clientId)
                .accessToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .lastRenewedAt(LocalDateTime.now())
                .build());

        access.setAccessToken(authResponse.getAccessToken());
        access.setRefreshToken(authResponse.getRefreshToken());
        access.setLastRenewedAt(LocalDateTime.now());
        repo.save(access);
        return authResponse;
    }
}
