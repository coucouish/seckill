package com.example.seckilldemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.mapper.GoodsMapper;
import com.example.seckilldemo.pojo.Goods;
import com.example.seckilldemo.service.IGoodsService;
import com.example.seckilldemo.vo.GoodsVo;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Tessa
 * @since 2023-05-03
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 获取商品列表
     * @return
     */
    @Override
    public List<GoodsVo> findGoodsVo() {
        System.out.println("GoodsServiceImpl");
        List<GoodsVo> goodsVo = null;
        try{
            goodsVo = goodsMapper.findGoodsVo();
        }catch(Throwable exceptionUtil){
            System.out.println(exceptionUtil);

        }

        return goodsVo;
    }

    /**
     * 获取商品详情
     * @param goodsId
     * @return
     */
    @Override
    public GoodsVo findGoodsVoByGoodsId(long goodsId) {
        return goodsMapper.findGoodsVoByGoodsId(goodsId);
    }
}
