package net.xdclass.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.enums.CouponCategoryEnum;
import net.xdclass.request.NewUserRequest;
import net.xdclass.service.CouponService;
import net.xdclass.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import springfox.documentation.spring.web.json.Json;

import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author ygk
 * @since 2024-05-19
 */
@RestController
@RequestMapping("/api/coupon/v1")
public class CouponController {
    @Autowired
    CouponService couponService;

    @ApiOperation("分页查询优惠券")
    @GetMapping("page_coupon")
    public JsonData pageCouponList(
            @ApiParam(value = "当前页") @RequestParam(value = "page", defaultValue = "1") int page,
            @ApiParam(value = "每页显示多少条") @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Map<String, Object> pageMap = couponService.pageCouponActivity(page, size);
        return JsonData.buildSuccess(pageMap);
    }

    @ApiOperation("领取优惠券")
    @GetMapping("add/promotion/{coupon_id}")
    public JsonData getPromotionCoupon(
            @ApiParam(value = "优惠券id", required = true) @PathVariable("coupon_id") long couponId){
        return couponService.getCoupon(couponId, CouponCategoryEnum.PROMOTION);
    }

    @ApiOperation("RPC-直接通过微服务之间调用进行通信-新用户领取新人优惠券")
    @PostMapping("new_user_coupon")
    public JsonData addNewUserCoupon(@ApiParam(value = "新用户对象") @RequestBody NewUserRequest newUserRequest){
        return couponService.initNewUserCoupon(newUserRequest);
    }

}

