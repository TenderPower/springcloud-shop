package net.xdclass.feign;

import io.swagger.annotations.ApiParam;
import net.xdclass.request.NewUserRequest;
import net.xdclass.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "xdclass-coupon-service")
public interface CouponFeignService {
    //用户微服务 与 优惠券微服务 之间通信

    /**
     * 新用户注册发放优惠券
     * @param newUserRequest
     * @return
     */
    @PostMapping("/api/coupon/v1/new_user_coupon")
    public JsonData addNewUserCoupon(@ApiParam(value = "新用户对象") @RequestBody NewUserRequest newUserRequest);
}
