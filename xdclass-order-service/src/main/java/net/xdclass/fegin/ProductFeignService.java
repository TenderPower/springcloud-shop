package net.xdclass.fegin;

import io.swagger.annotations.ApiParam;
import net.xdclass.request.LockProductRequest;
import net.xdclass.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "xdclass-product-service")
public interface ProductFeignService {
    /**
     * 获取用户在购物车中所选中的商品
     * 下单成功，也会将商品从购物车中删除
     *
     * @param productIdList
     * @return
     */
    @PostMapping("/api/cart/v1/confirm_order_cart_item")
    JsonData confirmOrderCartItems(@RequestBody List<Long> productIdList);

    /**
     * 锁定商品库存
     * @param lockProductRequest
     * @return
     */
    @PostMapping("/api/product/v1/lock_products")
    public JsonData lockProducts(@ApiParam("商品库存锁定") @RequestBody LockProductRequest lockProductRequest);

}
