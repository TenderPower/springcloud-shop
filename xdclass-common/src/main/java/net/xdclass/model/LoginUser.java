package net.xdclass.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginUser {
    /**
     * 主键
     */
    private Long id;
    /**
     * 姓名
     */
    private String name;
    /**
     * 头像
     */
    @JsonProperty("head_img")
    private String headImg;
    /**
     * 邮箱
     */
    private String mail;

    /**
     * Ip -- 防止JWT令牌token泄露后恶意使用
     */
    private String ip;
}
