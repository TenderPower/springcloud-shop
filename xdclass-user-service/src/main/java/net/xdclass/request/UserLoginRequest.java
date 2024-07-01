package net.xdclass.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Data;

@ApiModel(value = "登录对象",description = "登录请求对象")
@Data
public class UserLoginRequest {

    @ApiModelProperty(value = "邮箱", example = "954337256@qq.com")
    private String email;
    @ApiModelProperty(value = "密码", example = "123456")
    private String pwd;
}
