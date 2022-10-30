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
public class ApplyCodeReq {
    @NotNull
    private String phoneNumber;
    @ApiModelProperty("指定该验证码是用来注册还是用来登录的 1表示登录 2表示注册")
    @NotNull
    private Integer type;
}
