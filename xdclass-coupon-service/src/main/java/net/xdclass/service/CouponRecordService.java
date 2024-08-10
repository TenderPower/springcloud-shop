package net.xdclass.service;

import net.xdclass.model.CouponRecordMessage;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.util.JsonData;
import net.xdclass.vo.CouponRecordVO;

import java.util.Map;

public interface CouponRecordService {
    /**
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    Map<String, Object>  page(int page, int size);

    /**
     * 用户根据优惠券id 查找优惠券信息
     * @param recordId
     * @return
     */
    CouponRecordVO findeById(Long recordId);

    /**
     * 锁定优惠券
     * @param recordRequest
     * @return
     */
    JsonData lockCouponRecords(LockCouponRecordRequest recordRequest);

    /**
     * 释放优惠券
     * @param couponRecordMessage
     * @return
     */
    boolean releaseCouponRecord(CouponRecordMessage couponRecordMessage);
}
