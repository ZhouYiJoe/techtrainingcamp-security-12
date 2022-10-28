package com.catchyou.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RegisterReq {
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String phoneNumber;
    @NotNull
    private String verifyCode;
    @Valid
    @NotNull
    private Environment environment;
}
