package net.xdclass.service;

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
}
