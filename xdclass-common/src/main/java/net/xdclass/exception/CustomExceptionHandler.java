package net.xdclass.exception;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.util.JsonData;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 构建异常类的处理器
 */
@ControllerAdvice //将该处理器的Java类当成controller对待
@Slf4j //毕竟是异常 要打印的
public class CustomExceptionHandler {

    /**
     * 当系统中发生指定类型的异常时，将调用该方法来处理异常(系统自动的)
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
//    当一个方法被标记为@ExceptionHandler时，它必须接受一个异常对象作为参数，并返回一个响应对象。
//    当系统中发生指定类型的异常时，将调用该方法来处理异常(系统自动的)
    @ResponseBody //指定一个方法的返回值应该直接写入HTTP响应正文中，而不是被解析为视图
    public JsonData handle(Exception e) {
//        判断是不是自定义的异常接口
        if (e instanceof BizException) {
            BizException bizException = (BizException) e;
            log.error("[业务异常{}]", e);
//            向客服端响应msg
            return JsonData.buildCodeAndMsg(bizException.getCode(), bizException.getMsg());
        } else {
            log.error("[非业务异常{}]", e);
        }
//        返回 统一响应接口的类
        return JsonData.buildError("全局异常，未知错误");
    }
}
