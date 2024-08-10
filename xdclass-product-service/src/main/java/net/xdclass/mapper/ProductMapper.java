package net.xdclass.mapper;

import net.xdclass.model.ProductDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ygk
 * @since 2024-06-30
 */
public interface ProductMapper extends BaseMapper<ProductDO> {

    /**
     * 锁定对应商品的库存
     * @param productId
     * @param buyNum
     */
    int lockProductStock(@Param("productId") long productId,@Param("buyNum") int buyNum);
}
