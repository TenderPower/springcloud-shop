package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

//我觉得他就是DTO
@ApiModel(value = "用户注册对象", description = "用户注册请求对象")
@Data
public class UserRegisterRequest {

    @ApiModelProperty(value = "用户名", example = "YGK")
    private String name;

    @ApiModelProperty(value = "密码", example = "123456")
    private String pwd;

    @ApiModelProperty(value = "头像", example = "https://xdclass-2024shop-img.oss-cn-beijing.aliyuncs.com/userImage/2024/05/10/7a6e738381bc42f38940c2df23e43053.jpg")
    @JsonProperty("head_img")//前端的变量名为head_img；后端的变量名是headImg
    private String headImg;

    @ApiModelProperty(value = "用户个性签名", example = "态度决定一切")
    private String slogan;

    @ApiModelProperty(value = "0表示女，1表示男", example = "1")
    private String sex;

    @ApiModelProperty(value = "邮箱", example = "954337256@qq.com")
    private String mail;

    @ApiModelProperty(value = "验证码", example = "验证码")
    private String code;

}
