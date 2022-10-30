package com.catchyou.pojo.dto;

import io.swagger.annotations.ApiModelProperty;
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
public class LoginWithPhoneReq {
    @NotNull
    private String phoneNumber;
    @ApiModelProperty("验证码")
    @NotNull
    private String verifyCode;
}
