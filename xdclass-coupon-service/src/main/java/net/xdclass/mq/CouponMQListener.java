package net.xdclass.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.CouponRecordMessage;
import net.xdclass.service.CouponRecordService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RabbitListener(queues = "${mqconfig.coupon_release_queue}")
public class CouponMQListener {
    @Autowired
    private CouponRecordService couponRecordService;

    /**
     * 重复消费--幂等性
     * 消息失败，达到重新入队的最大次数：
     *  如果消息失败，不重新入队，可以记录日志，然后插入到数据库中进行人工排除
     * @param couponRecordMessage
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    public void releaseCouponRecord(CouponRecordMessage couponRecordMessage, Message message, Channel channel) throws IOException {
        log.info("监听到消息，消息内容：{}", couponRecordMessage);
        long msgTag = message.getMessageProperties().getDeliveryTag();

        boolean flag = couponRecordService.releaseCouponRecord(couponRecordMessage);
        try {
            if (flag) {
//            确认消息消费成功
                channel.basicAck(msgTag, false);
            }else {
                log.error("释放优惠券失败 flag=false,{}",couponRecordMessage);
                channel.basicReject(msgTag, true);
            }
        } catch (IOException e) {
            log.error("释放优惠券异常，{}",e.getMessage());
            channel.basicReject(msgTag, true);
        }

    }
}
