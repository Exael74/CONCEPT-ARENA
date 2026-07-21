package com.conceptarena.auth.web;

import com.conceptarena.auth.app.UserRepository;
import com.conceptarena.auth.app.bus.CommandBus;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.Username;
import com.conceptarena.auth.domain.command.UpdateUsernameCommand;
import com.conceptarena.auth.domain.error.DuplicateUsernameException;
import com.conceptarena.auth.web.dto.ApiResponse;
import com.conceptarena.kernel.error.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * The authenticated user's own profile. Protected by SecurityConfig (requires a valid JWT,
 * unlike the rest of /api/auth/** which is public) — the userId is always taken from the token
 * subject (authentication.getName()), never trusted from the request body/path.
 */
@RestController
@RequestMapping("/api/auth/me")
public class ProfileController {

    private final UserRepository userRepository;
    private final CommandBus commandBus;

    public ProfileController(UserRepository userRepository, CommandBus commandBus) {
        this.userRepository = userRepository;
        this.commandBus = commandBus;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> me(Authentication authentication) {
        return userRepository.findById(authentication.getName())
            .map(user -> ResponseEntity.ok(ApiResponse.success("Profile", toResponse(user))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("User not found")));
    }

    @PatchMapping("/username")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateUsername(
            Authentication authentication, @RequestBody UpdateUsernameRequest body) {
        try {
            commandBus.dispatch(new UpdateUsernameCommand(authentication.getName(), new Username(body.username())));
        } catch (DuplicateUsernameException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
        return userRepository.findById(authentication.getName())
            .map(user -> ResponseEntity.ok(ApiResponse.success("Username updated", toResponse(user))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("User not found")));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(
            user.getId().value(), user.getEmail().value(), user.getUsername().value(),
            user.isActive(), user.getRegisteredAt().toString());
    }

    public record ProfileResponse(String id, String email, String username, boolean active, String registeredAt) {}
    public record UpdateUsernameRequest(String username) {}
}
