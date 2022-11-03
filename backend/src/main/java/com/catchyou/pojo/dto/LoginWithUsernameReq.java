package com.catchyou.pojo.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LoginWithUsernameReq {
    @NotNull
    private String username;
    @ApiModelProperty("经过公钥加密并进行了Base64编码的密码")
    @NotNull
    private String password;
    @ApiModelProperty("经过Base64编码的公钥")
    @NotNull
    private String publicKeyBase64;
}
