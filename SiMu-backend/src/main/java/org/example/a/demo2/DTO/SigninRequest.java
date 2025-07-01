
package org.example.a.demo2.DTO;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class SigninRequest {
    private String username;
    private String password;
}
