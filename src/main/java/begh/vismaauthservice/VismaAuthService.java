package begh.vismaauthservice;

import com.nimbusds.oauth2.sdk.token.Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class VismaAuthService {
    @Value("${spring.security.oauth2.client.registration.visma.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.visma.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.visma.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate;

    public VismaAuthService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public Tokens getTokens() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials"); // Adjust the grant_type if needed

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Tokens> response = restTemplate.postForEntity(tokenUri, request, Tokens.class);
        return response.getBody();
    }

    public Tokens refreshTokens() {
        // Retrieve refresh token from the database
        String refreshToken = ""; // Retrieve from the database

        // Prepare the request to refresh the token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // Send request to refresh token
        ResponseEntity<Tokens> response = restTemplate.postForEntity(tokenUri, request, Tokens.class);
        Tokens tokens = response.getBody();

        // Save new tokens to the database
        // saveTokens(tokens);

        return tokens;
    }
}
