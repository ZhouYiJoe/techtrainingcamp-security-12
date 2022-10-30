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
@ApiModel("注册接口响应体")
public class RegisterRes {
    @ApiModelProperty("token的有效时长")
    private Long expireTime;
    @ApiModelProperty("token")
    private String sessionId;
}
