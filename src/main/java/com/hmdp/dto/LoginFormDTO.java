package com.hmdp.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
