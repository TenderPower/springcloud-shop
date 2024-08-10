package net.xdclass.mapper;

import net.xdclass.model.CouponTaskDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ygk
 * @since 2024-07-25
 */
public interface CouponTaskMapper extends BaseMapper<CouponTaskDO> {
    /**
     * 批量插入优惠卷记录的状态
     * @param couponTaskDOList
     * @return
     */
    int insertBatch(@Param("couponTaskList") List<CouponTaskDO> couponTaskDOList);
}
