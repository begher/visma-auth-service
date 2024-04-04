package begh.vismaauthservice;

import com.nimbusds.oauth2.sdk.token.Tokens;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    private final VismaAuthService vismaAuthService;

    public Controller(VismaAuthService vismaAuthService) {
        this.vismaAuthService = vismaAuthService;
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<Tokens> refreshToken() {
        Tokens tokens = vismaAuthService.refreshTokens();
        if (tokens != null) {
            return ResponseEntity.ok(tokens);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/token")
    public ResponseEntity<Tokens> getToken() {
        return ResponseEntity.ok(vismaAuthService.getTokens());
    }
}
