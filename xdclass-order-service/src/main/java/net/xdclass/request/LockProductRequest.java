package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel(value = "商品锁定对象", description = "商品锁定对象协议")
@Data
public class LockProductRequest {
    @ApiModelProperty(value = "订单号id", example = "1234567890")
    @JsonProperty("order_out_trade_no")
    private String orderOutTradeNo;

    @ApiModelProperty(value = "订单商品列表")
    @JsonProperty("order_item_list")
    private List<OrderItemRequest> orderItemList;
}
