package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("购物车商品")
@Data
public class CartItemRequest {

    @ApiModelProperty(value = "商品id",example = "2")
    @JsonProperty("product_id")
    private Long productId;
    @ApiModelProperty(value = "购买数量",example = "1")
    @JsonProperty("buy_num")
    private Integer buyNum;
}
