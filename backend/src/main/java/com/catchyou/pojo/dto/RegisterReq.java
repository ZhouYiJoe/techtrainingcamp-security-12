package com.catchyou.pojo.dto;

import io.swagger.annotations.ApiModel;
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
@ApiModel("注册请求体")
public class RegisterReq {
    @NotNull
    private String username;
    @ApiModelProperty("经过公钥加密并进行了Base64编码的密码")
    @NotNull
    private String password;
    @NotNull
    private String phoneNumber;
    @ApiModelProperty("验证码")
    @NotNull
    private String verifyCode;
    @ApiModelProperty("经过Base64编码的公钥")
    @NotNull
    private String publicKeyBase64;
}
