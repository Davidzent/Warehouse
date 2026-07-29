package com.warehouse.receiving.web;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Profile("dev")
@RestController
@RequestMapping("/api/auth")
public class DevTokenController {

    // CLERK may post receipts; 
    // VIEWER may only read - Drives the 403 demo
    public record DevTokenRequest(
            @NotBlank String username,
            @NotNull @Pattern(regexp = "CLERK|VIEWER", message = "role must be CLERK or VIEWER") String role) {}

    public record DevTokenResponse(String token, String username, String role, Instant expiresAt) {}

    private final JwtEncoder jwtEncoder;

    public DevTokenController(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/dev-token")
    public DevTokenResponse issueToken(@Valid @RequestBody DevTokenRequest request) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(8, ChronoUnit.HOURS);

        String grantedRole = "CLERK".equals(request.role()) ? "WAREHOUSE_CLERK" : "VIEWER";

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("warehouse-receiving-dev")
                .subject(request.username())      // becomes receipt.received_by
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", List.of(grantedRole))
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        return new DevTokenResponse(token, request.username(), request.role(), expiresAt);
    }
}
