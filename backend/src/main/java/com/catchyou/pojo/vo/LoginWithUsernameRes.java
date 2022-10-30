package com.catchyou.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ApiModel("loginWithUsername接口响应体")
public class LoginWithUsernameRes {
    @ApiModelProperty("密码输入错误次数过多后限制多长时间内用户无法再次登录，单位为毫秒")
    private Integer banTime;
    @ApiModelProperty("decisionType=2 因为密码 5 次错误，封禁 1 分钟\n" +
            "decisionType=2 因为密码 10 次错误，封禁 5 分钟\n" +
            "decisionType=3 因为密码 15 次错误，永久封禁\n" +
            "decisionType=4 因为之前密码输入次数太多，被禁止登录")
    private Integer decisionType;
    @ApiModelProperty("token的有效时长")
    private Long expireTime;
    @ApiModelProperty("token")
    private String sessionId;
}
