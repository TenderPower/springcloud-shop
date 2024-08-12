package net.xdclass.fegin;

import io.swagger.annotations.ApiParam;
import net.xdclass.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "xdclass-coupon-service")
public interface UserFeignService {
    /**
     * 查询用户地址
     * @param id
     * @return
     */
    @GetMapping("/api/address/v1/find/{address_id}")
    JsonData detail(@ApiParam(value = "地址id", required = true) @PathVariable("address_id") Long id);
}
