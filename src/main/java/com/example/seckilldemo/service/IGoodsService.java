package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.pojo.Goods;
import com.example.seckilldemo.vo.GoodsVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Tessa
 * @since 2023-05-03
 */
public interface IGoodsService extends IService<Goods> {


    /**
     * 获取商品列表
     * @return
     */

    List<GoodsVo> findGoodsVo();

    /**
     * 获取商品详情
     * @param goodsId
     * @return
     */
    GoodsVo findGoodsVoByGoodsId(long goodsId);
}
