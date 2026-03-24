package com.camerarental.backend.controller;



import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.security.jwt.JwtBlacklistService;
import com.camerarental.backend.security.jwt.JwtUtils;
import com.camerarental.backend.model.entity.Role;
import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.model.entity.enums.AppRole;
import com.camerarental.backend.repository.RoleRepository;
import com.camerarental.backend.repository.UserRepository;
import com.camerarental.backend.security.services.UserDetailsImpl;
import com.camerarental.backend.security.request.LoginRequest;
import com.camerarental.backend.security.request.SignupRequest;
import com.camerarental.backend.security.response.MessageResponse;
import com.camerarental.backend.security.response.UserInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RoleRepository roleRepository;


    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (AuthenticationException exception) {
            log.warn("auth_failed username={} reason={}",
                    loginRequest.getUsername(),
                    exception.getClass().getSimpleName());

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Bad credentials",
                            "status", false
                    ));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        UserInfoResponse userInfo = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles
        );

        // 🔑 Create a JWT token for Authorization: Bearer
        String token = jwtUtils.generateJwtToken(userDetails);

        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", token);
        body.put("tokenType", "Bearer");
        body.put("user", userInfo);

        return ResponseEntity.ok(body);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUserName(signupRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already taken!"));
        }

        User user = new User(
                signupRequest.getUsername(),
                signupRequest.getEmail(),
                encoder.encode(signupRequest.getPassword())
        );

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER missing from database"));
        roles.add(userRole);

        user.setRoles(roles);
        userRepository.save(user);
        return new ResponseEntity<>(new MessageResponse("User registered successfully!"), HttpStatus.CREATED);
    }

    @GetMapping("/username")
    @PreAuthorize("isAuthenticated()")
    public String currentUserName(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("No authentication found for authenticated endpoint");
        }
        return authentication.getName();
    }


    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles
        );

        return ResponseEntity.ok().body(response);
    }

    /**
     * For Bearer tokens, "signout" is a client-side concern.
     * We now also blacklist the presented token (by jti) so it is rejected
     * for the remainder of its lifetime.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/signout")
    public ResponseEntity<?> signOutUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtUtils.getJtiFromToken(token);
                jwtBlacklistService.blacklist(jti);
            } catch (Exception e) {
                log.warn("signout_token_parse_failed: {}", e.getMessage());
            }
        } else {
            log.debug("signout_without_bearer_header");
        }

        return ResponseEntity.ok(new MessageResponse(
                "You have been signed out. Token has been invalidated; please remove it on the client.")
        );
    }

}
