package com.example.seckilldemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.config.RedisConfig;
import com.example.seckilldemo.mapper.SeckillOrderMapper;
import com.example.seckilldemo.pojo.SeckillOrder;
import com.example.seckilldemo.pojo.User;
import com.example.seckilldemo.service.ISeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Tessa
 * @since 2023-05-03
 */
@Service
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements ISeckillOrderService {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 获取秒杀结果
     * @param user
     * @param goodsId
     * @return orderId: 秒杀成功，-1 - 秒杀失败，0 - 排队中
     */
    @Override
    public long getResult(User user, Long goodsId) {
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));
        if(seckillOrder != null){
            return seckillOrder.getOrderId();
        }else if(redisTemplate.hasKey("isStockEmpty:" + goodsId)){
            return -1L;
        }else{
            return 0L;
        }
    }
}
