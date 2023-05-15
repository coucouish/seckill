package com.example.seckilldemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckilldemo.pojo.Goods;
import com.example.seckilldemo.vo.GoodsVo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Tessa
 * @since 2023-05-03
 */
@Component
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 获取商品列表
     * @return
     */
    List<GoodsVo> findGoodsVo();

    /**
     * 获取商品详情
     * @return
     */
    GoodsVo findGoodsVoByGoodsId(long goodsId);
}
