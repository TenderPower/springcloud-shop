package net.xdclass.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.xdclass.enums.BizCodeEnum;

/**
 * 工具包
 */
@Data
@AllArgsConstructor //生成参数构造函数
@NoArgsConstructor //生成无参构造函数
//其实就是向客户端返回 一个统一的响应对象，这个对象就是自定应的Java类
public class JsonData {

    /**
     * 状态码 0 表示成功，1表示处理中，-1表示失败
     */

    private Integer code;
    /**
     * 数据
     */
    private Object data;
    /**
     * 描述
     */
    private String msg;

    /**
     * 获取远程调用数据
     * 支持多单词下划线转驼峰（序列化和反序列化）
     *
     * TypeReference<T> 是一个泛型类，它本身不包含任何方法或字段，它的主要作用就是提供一个类型标记，
     * 使得反序列化库能够知道如何创建正确的泛型实例。
     *
     * @param typeReference
     * @param <T>
     * @return
     */
    public <T> T getData(TypeReference<T> typeReference) {
//      Java 对象转换为 JSON 格式的字符串。这个方法非常有用，
//      特别是在需要将 Java 对象序列化为 JSON 字符串以便在网络上传输或者存储的时候

//       假设你有一个 JSON 字符串 jsonString，
//       你可以使用 JSON.parseObject(jsonString, clazz) 方法将其转换为指定类型的 Java 对象
        return JSON.parseObject(JSON.toJSONString(data), typeReference);
    }

    /**
     * 成功，不传入数据
     *
     * @return
     */
    public static JsonData buildSuccess() {
        return new JsonData(0, null, null);
    }

    /**
     * 成功，传入数据
     *
     * @param data
     * @return
     */
    public static JsonData buildSuccess(Object data) {
        return new JsonData(0, data, null);
    }

    /**
     * 成功，传入描述信息
     *
     * @param msg
     * @return
     */
    public static JsonData buildSuccess(String msg) {
        return new JsonData(0, null, msg);
    }

    /**
     * 失败，传入描述信息
     *
     * @param msg
     * @return
     */
    public static JsonData buildError(String msg) {
        return new JsonData(-1, null, msg);
    }


    /**
     * 自定义状态码和错误信息
     *
     * @param code
     * @param msg
     * @return
     */
    public static JsonData buildCodeAndMsg(int code, String msg) {
        return new JsonData(code, null, msg);
    }

    /**
     * 传入枚举，返回信息
     *
     * @param codeEnum
     * @return
     */
    public static JsonData buildResult(BizCodeEnum codeEnum) {
        return JsonData.buildCodeAndMsg(codeEnum.getCode(), codeEnum.getMessage());
    }
}