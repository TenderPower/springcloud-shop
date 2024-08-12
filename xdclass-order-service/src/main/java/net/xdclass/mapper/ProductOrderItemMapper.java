package net.xdclass.mapper;

import net.xdclass.model.ProductOrderItemDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ygk
 * @since 2024-07-04
 */
public interface ProductOrderItemMapper extends BaseMapper<ProductOrderItemDO> {

    /**
     * 批量插入
     * @param list
     */
    void insertBatch(@Param("list") List<ProductOrderItemDO> list);
}
