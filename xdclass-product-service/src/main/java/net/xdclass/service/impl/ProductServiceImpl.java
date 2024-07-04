package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.CouponCategoryEnum;
import net.xdclass.enums.CouponPublishEnum;
import net.xdclass.mapper.ProductMapper;
import net.xdclass.model.ProductDO;
import net.xdclass.service.ProductService;
import net.xdclass.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;

    /**
     * 商品首页分页列表接口
     *
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> pageProductActivity(int page, int size) {
        //        分页场景的使用：
//        1.new Page对象
        Page<ProductDO> pageInfo = new Page<>(page, size);
//        2.开始查询 并返回结果
        IPage<ProductDO> productDOIPage = productMapper.selectPage(pageInfo, null);
//        3.映射到Map中
        Map<String, Object> pageMap = new HashMap<>(3);
//        总条数
        pageMap.put("total_record", productDOIPage.getTotal());
//        总页数
        pageMap.put("total_page", productDOIPage.getPages());
        pageMap.put("current_data", productDOIPage.getRecords()
//                将Do 转为Vo 传给前端
                .stream()
                .map(obj -> beanProcess(obj)).collect(Collectors.toList()));

        return pageMap;
    }

    /**
     * 根据商品id查询商品详情
     * @param productId
     * @return
     */
    @Override
    public ProductVO findDetailById(long productId) {
        ProductDO productDO = productMapper.selectById(productId);
        return beanProcess(productDO);
    }

    /**
     * 根据商品id查询商品详情
     * @param productIdList
     * @return
     */
    @Override
    public List<ProductVO> findDetailByIds(List<Long> productIdList) {
        List<ProductDO> productDOList =productMapper.selectList(new QueryWrapper<ProductDO>().in("id", productIdList));
        return productDOList.stream().map(obj -> beanProcess(obj)).collect(Collectors.toList());
    }

    /**
     * 将DO对象 转为 VO对象
     *
     * @param productDO
     * @return
     */
    private ProductVO beanProcess(ProductDO productDO) {
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(productDO, productVO);
//        商品剩余的库存
        productVO.setStock(productDO.getStock()-productDO.getLockStock());
        return productVO;
    }
}
