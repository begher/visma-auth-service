package begh.vismaauthservice;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class Controller {

    private final AuthService authService;

    @GetMapping("/authorize")
    public void redirectToAuthorization(HttpServletResponse response) throws IOException {
        authService.redirectToAuthorization(response);
    }

    @GetMapping("/refresh")
    public Mono<AuthResponse> refreshToken() {
        return authService.refreshToken();
    }
    @GetMapping("/login/oauth2/code/visma")
    public Mono<AuthResponse> handleOAuthRedirect(@RequestParam("code") String code) {
            return authService.handleOAuthRedirect(code);
    }

    @GetMapping("/tokens")
    public Mono<AuthResponse> getTokens(){
        return authService.getTokens();
    }
}
