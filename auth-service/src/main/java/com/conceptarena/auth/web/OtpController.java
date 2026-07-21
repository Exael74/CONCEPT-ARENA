package com.conceptarena.auth.web;

import com.conceptarena.auth.app.bus.CommandBus;
import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.command.RequestOtpCommand;
import com.conceptarena.auth.domain.command.VerifyOtpCommand;
import com.conceptarena.auth.domain.error.InvalidOtpException;
import com.conceptarena.auth.web.dto.ApiResponse;
import com.conceptarena.kernel.error.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Email verification (account activation), not an alternative login: registration creates an
 * INACTIVE account, and verify is what activates it (and, since the user is now proven to own the
 * email, also returns a JWT so the client doesn't need a separate login call right after).
 *   POST /api/auth/otp/request  { "email": "..." }            -> (re)sends a 6-digit code
 *   POST /api/auth/otp/verify   { "email": "...", "code": "" } -> activates the account, returns a JWT
 *
 * request always returns 200 with the same body whether the email is unregistered or already
 * verified (anti-enumeration) — see RequestOtpCommandHandler. verify returns 401 for any
 * wrong/expired/exhausted code. Both paths are IP-rate-limited (RateLimitingFilter).
 */
@RestController
@RequestMapping("/api/auth/otp")
public class OtpController {

    private final CommandBus commandBus;

    public OtpController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> request(@RequestBody RequestOtpRequest body) {
        try {
            commandBus.dispatch(new RequestOtpCommand(new Email(body.email())));
        } catch (DomainException e) {
            // Invalid email format is the one thing we surface (400) — it's a client input error,
            // not account state. Everything else stays generic to avoid enumeration.
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(
            "If that email is registered, a code has been sent.", null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verify(@RequestBody VerifyOtpRequest body) {
        if (body.code() == null || body.code().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Code must not be empty"));
        }
        try {
            String token = commandBus.dispatch(new VerifyOtpCommand(new Email(body.email()), body.code().trim()));
            return ResponseEntity.ok(ApiResponse.success("Account verified — use token as Bearer", token));
        } catch (InvalidOtpException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    public record RequestOtpRequest(String email) {}
    public record VerifyOtpRequest(String email, String code) {}
}
