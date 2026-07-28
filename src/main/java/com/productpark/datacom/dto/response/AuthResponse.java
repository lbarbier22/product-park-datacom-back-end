package com.productpark.datacom.dto.response;

import com.productpark.datacom.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String login;
    private String firstname;
    private String lastname;
    private Role role;

}
