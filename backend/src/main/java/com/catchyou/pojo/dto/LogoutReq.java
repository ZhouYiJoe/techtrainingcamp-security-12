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
public class LogoutReq {
    @NotNull
    private String sessionId;
    @ApiModelProperty("1代表登出，2代表注销")
    @NotNull
    public Integer actionType;
    @Valid
    @NotNull
    public Environment environment;
}
