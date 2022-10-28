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
public class AuthRes {
    private String sessionId;
    @ApiModelProperty("session的有效时长，单位为秒")
    private Integer expireTime;
    @ApiModelProperty("不同的数值代表不同类型的登录请求结果")
    private Integer decisionType;
    @ApiModelProperty("密码输入错误次数过多后限制多长时间内用户无法再次登录，单位为毫秒")
    private Integer banTime;
}
