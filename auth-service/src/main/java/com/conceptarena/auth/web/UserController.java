package com.conceptarena.auth.web;

import com.conceptarena.auth.app.bus.CommandBus;
import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.command.LoginUserCommand;
import com.conceptarena.auth.domain.command.RegisterUserCommand;
import com.conceptarena.auth.domain.error.DuplicateEmailException;
import com.conceptarena.auth.domain.error.InvalidCredentialsException;
import com.conceptarena.auth.web.dto.ApiResponse;
import com.conceptarena.kernel.error.DomainException;
import com.conceptarena.kernel.valueobject.PasswordHash;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final CommandBus commandBus;

    public UserController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterUserRequest request) {
        try {
            var command = new RegisterUserCommand(
                new Email(request.email()),
                PasswordHash.fromPlain(request.password())
            );
            String userId = commandBus.dispatch(command);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered", userId));
        } catch (DuplicateEmailException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
        } catch (DomainException e) {
            // A1: an invalid email (Email value object throws DomainException) is a client error —
            // return 400, not the generic 500 the unhandled DomainException used to fall through to.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginUserRequest request) {
        try {
            var command = new LoginUserCommand(
                new Email(request.email()),
                PasswordHash.fromPlain(request.password())
            );
            String token = commandBus.dispatch(command);
            return ResponseEntity.ok(ApiResponse.success("Login successful — use token as Bearer", token));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        } catch (DomainException e) {
            // A1: invalid email format on login is a 400, not a 500.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    public record RegisterUserRequest(String email, String password) {}
    public record LoginUserRequest(String email, String password) {}
}
