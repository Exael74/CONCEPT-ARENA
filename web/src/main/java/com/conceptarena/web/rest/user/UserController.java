package com.conceptarena.web.rest.user;

import com.conceptarena.app.bus.CommandBus;
import com.conceptarena.core.shared.error.DuplicateEmailException;
import com.conceptarena.core.shared.error.InvalidCredentialsException;
import com.conceptarena.core.shared.valueobject.Email;
import com.conceptarena.core.shared.valueobject.PasswordHash;
import com.conceptarena.core.user.command.LoginUserCommand;
import com.conceptarena.core.user.command.RegisterUserCommand;
import com.conceptarena.web.rest.dto.ApiResponse;
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    public record RegisterUserRequest(String email, String password) {}
    public record LoginUserRequest(String email, String password) {}
}
