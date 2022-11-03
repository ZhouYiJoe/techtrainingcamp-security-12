package com.catchyou.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

//由于所有的返回都有code和message两个参数，因此做一个简单的封装
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ApiModel("通用返回结果")
public class CommonResult<T> {
    @ApiModelProperty("0表示请求处理成功，1表示失败")
    private Integer code;
    private String message;
    //数据主体
    private T data;

    public CommonResult(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> CommonResult<T> ok(T data) {
        return new CommonResult<>(0, "成功", data);
    }
}
