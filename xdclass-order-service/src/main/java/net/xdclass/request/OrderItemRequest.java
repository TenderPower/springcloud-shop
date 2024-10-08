package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("订单商品")
@Data
public class OrderItemRequest {
    @ApiModelProperty(value = "商品id",example = "1")
    @JsonProperty("product_id")
    private long productId;

    @ApiModelProperty(value = "购买数量",example = "1")
    @JsonProperty("buy_num")
    private int buyNum;
}
