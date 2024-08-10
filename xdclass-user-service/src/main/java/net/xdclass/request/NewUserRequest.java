package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "新用户请求对象", description = "新用户请求对象")
public class NewUserRequest {

    @JsonProperty("user_id")
    private long userId;
    private String name;
}
