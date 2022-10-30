package com.catchyou.pojo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ApplyCodeRes {
    @ApiModelProperty("验证码")
    private String verifyCode;
    @ApiModelProperty("验证码的有效时长，单位为秒")
    private Integer expireTime;
    @ApiModelProperty("不同的数值代表不同类型的登录请求结果")
    private Integer decisionType;
}
