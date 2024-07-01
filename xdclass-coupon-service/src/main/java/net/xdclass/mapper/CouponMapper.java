package net.xdclass.mapper;

import net.xdclass.model.CouponDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author ygk
 * @since 2024-05-19
 */
public interface CouponMapper extends BaseMapper<CouponDO> {

    /**
     * 扣减优惠券库存
     *
     * @param couponId
     * @return
     */
//    @Param("参数名A") 参数类型  参数名B
//    在xml中使用参数名A 来表示DAO层中的参数名B
    int reduceStock(@Param("couponId") long couponId);
}
