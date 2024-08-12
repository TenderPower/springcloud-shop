package net.xdclass.fegin;

import io.swagger.annotations.ApiParam;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("xdclass-coupon-service")
public interface CouponFeignService {
    /**
     * 查看用户优惠券是否可用，防止水平权限越权（方法中加入用户id ，防止水平权限攻击）
     *
     * @param recordId
     * @return
     */
    @GetMapping("/api/coupon_record/v1/detail/{record_id}")
    JsonData findUserCouponRecordById(@ApiParam(value = "优惠券记录id", required = true) @PathVariable("record_id") Long recordId);

    /**
     * 锁定优惠券
     *
     * @param recordRequest
     * @return
     */
    @PostMapping("/api/coupon_record/v1/lock_records")
    JsonData lockCouponRecord(@ApiParam(value = "锁定优惠券请求对象") @RequestBody LockCouponRecordRequest recordRequest);
}