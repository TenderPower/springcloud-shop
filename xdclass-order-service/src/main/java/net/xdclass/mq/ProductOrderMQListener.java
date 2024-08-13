package net.xdclass.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.OrderMessage;
import net.xdclass.service.ProductOrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RabbitListener(queues = "${mqconfig.order_close_queue}")
public class ProductOrderMQListener {
    @Autowired
    private ProductOrderService productOrderService;

    /**
     * 重复消费--幂等性
     * 消息失败，达到重新入队的最大次数：
     *  如果消息失败，不重新入队，可以记录日志，然后插入到数据库中进行人工排除
     * @param orderMessage
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    public void releaseCouponRecord(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        log.info("监听到消息，消息内容：{}", orderMessage);
        long msgTag = message.getMessageProperties().getDeliveryTag();

        boolean flag = productOrderService.closeProductOrder(orderMessage);
        try {
            if (flag) {
//            确认消息消费成功
                channel.basicAck(msgTag, false);
            }else {
                log.error("释放优惠券失败 flag=false,{}",orderMessage);
                channel.basicReject(msgTag, true);
            }
        } catch (IOException e) {
            log.error("释放优惠券异常，{}",e.getMessage());
            channel.basicReject(msgTag, true);
        }

    }
}
