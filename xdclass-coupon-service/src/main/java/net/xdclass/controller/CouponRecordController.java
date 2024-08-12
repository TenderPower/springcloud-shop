package net.xdclass.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.service.CouponRecordService;
import net.xdclass.util.JsonData;
import net.xdclass.vo.CouponRecordVO;
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
@RequestMapping("/api/coupon_record/v1")
public class CouponRecordController {

    @Autowired
    private CouponRecordService couponRecordService;

    @ApiOperation("分页查询 个人的优惠券列表")
    @GetMapping("page")
    public JsonData page(@ApiParam(value = "当前页") @RequestParam(value = "page", defaultValue = "1") int page,
                         @ApiParam(value = "每页显示多少条") @RequestParam(value = "size", defaultValue = "20") int size) {

        Map<String, Object> pageMap = couponRecordService.page(page, size);
        return JsonData.buildSuccess(pageMap);
    }

    @ApiOperation("用户查询指定优惠券记录信息")
    @GetMapping("/detail/{record_id}")
    public JsonData findUserCouponRecordById(@ApiParam(value = "优惠券记录id", required = true) @PathVariable("record_id") Long recordId) {

        CouponRecordVO couponRecordVO = couponRecordService.findeById(recordId);
        return couponRecordVO==null ? JsonData.buildResult(BizCodeEnum.COUPON_NO_EXITS)
                : JsonData.buildSuccess(couponRecordVO);
    }

    @ApiOperation("锁定优惠券记录")
    @PostMapping("lock_records")
    public JsonData lockCouponRecord(@ApiParam(value = "锁定优惠券请求对象") @RequestBody LockCouponRecordRequest recordRequest) {
        JsonData jsonData = couponRecordService.lockCouponRecords(recordRequest);
        return jsonData;
    }

}

