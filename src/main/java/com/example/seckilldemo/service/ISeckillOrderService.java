package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.pojo.SeckillOrder;
import com.example.seckilldemo.pojo.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Tessa
 * @since 2023-05-03
 */
public interface ISeckillOrderService extends IService<SeckillOrder> {

    /**
     * 获取秒杀结果
     * @param user
     * @param goodsId
     * @return orderId: 秒杀成功，-1 - 秒杀失败，0 - 排队中
     */
    long getResult(User user, Long goodsId);
}
