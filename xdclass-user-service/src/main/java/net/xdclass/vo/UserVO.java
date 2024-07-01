package net.xdclass.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserVO {

    private Long id;
    /**
     * 昵称
     */
    private String name;


    /**
     * 头像
     */
    @JsonProperty("head_img")
    //向前端返回的变量就是head_img
    //向后端穿的变量也是head_img
    private String headImg;

    /**
     * 用户签名
     */
    private String slogan;

    /**
     * 0表示女，1表示男
     */
    private Integer sex;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 邮箱
     */
    private String mail;

}
