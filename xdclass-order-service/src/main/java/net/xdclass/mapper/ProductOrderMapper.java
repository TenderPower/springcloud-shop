package net.xdclass.mapper;

import net.xdclass.model.ProductOrderDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ygk
 * @since 2024-07-04
 */
public interface ProductOrderMapper extends BaseMapper<ProductOrderDO> {

    /**
     * 修改订单支付状态
     * @param outTradeNo
     * @param newState
     * @param oldState
     */
    void updateOrderPayState(@Param("out_trade_no") String outTradeNo, @Param("new_state") String newState, @Param("old_state") String oldState);
}
