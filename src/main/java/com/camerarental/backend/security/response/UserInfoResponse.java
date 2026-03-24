package com.camerarental.backend.security.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {
    private UUID id;
    private String username;
    private List<String> roles;
}
