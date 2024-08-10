package net.xdclass.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.ProductOrderItemDO;
import net.xdclass.mapper.ProductOrderItemMapper;
import net.xdclass.service.ProductOrderItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ygk
 * @since 2024-07-04
 */
@Service
@Slf4j
public class ProductOrderItemServiceImpl extends ServiceImpl<ProductOrderItemMapper, ProductOrderItemDO> implements ProductOrderItemService {

}
