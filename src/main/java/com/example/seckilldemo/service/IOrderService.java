package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.pojo.Order;
import com.example.seckilldemo.pojo.User;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.OrderDetailVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Tessa
 * @since 2023-05-03
 */
public interface IOrderService extends IService<Order> {

    /**
     * 秒杀
     * @param user
     * @param goods
     * @return
     */
    Order seckill(User user, GoodsVo goods);

    /**
     * 订单详情
     * @param orderId
     * @return
     */
    OrderDetailVo detail(long orderId);

    /**
     * 获取秒杀地址
     * @param user
     * @param goodsId
     * @return
     */
    String createPath(User user, Long goodsId);

    /**
     * 校验秒杀地址
     *
     * @param user
     * @param goodsId
     * @param path
     * @return
     */
    boolean checkPath(User user, long goodsId, String path);

    /**
     * 校验验证码
     * @param user
     * @param goodsId
     * @param captcha
     * @return
     */
    boolean checkCaptcha(User user, Long goodsId, String captcha);
}
