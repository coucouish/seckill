package com.example.seckilldemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.seckilldemo.config.AccessLimit.AccessLimit;
import com.example.seckilldemo.exception.GlobalException;
import com.example.seckilldemo.pojo.Order;
import com.example.seckilldemo.pojo.SeckillMessage;
import com.example.seckilldemo.pojo.SeckillOrder;
import com.example.seckilldemo.pojo.User;
import com.example.seckilldemo.rabbitmq.MQSender;
import com.example.seckilldemo.service.IGoodsService;
import com.example.seckilldemo.service.IOrderService;
import com.example.seckilldemo.service.ISeckillOrderService;
import com.example.seckilldemo.utils.JsonUtil;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.RespBean;
import com.example.seckilldemo.vo.RespBeanEnum;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀
 */
@Slf4j
@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {
    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private ISeckillOrderService seckillOrderService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MQSender mqSender;

    @Autowired
    private RedisScript<Long> script;

    private Map<Long,Boolean> emptyStockMap = new HashMap<>();
    /**
     * 秒杀
     * windows（30000样本）
     *              优化前QPS：449.5
     *      页面缓存优化后 QPS：523.0
     *         服务优化后 QPS：626.4
     *
     * linux（30000样本）
     *              优化前QPS：165.9
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping("/doSeckill2")
    public String doSeckill2(Model model, User user, long goodsId){
        System.out.println("start");
        if(user == null){
            return "login";
        }
        System.out.println("login!");
        model.addAttribute("user",user);
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        System.out.println("create user");
        //判断库存
        if(goods.getStockCount() <= 0){
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "seckillFail";
        }
        System.out.println("有库存");

        //判断是都重复抢购
        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>()
                .eq("user_id", user.getId()).eq("goods_id", goodsId));
        if(seckillOrder != null){
            model.addAttribute("errmsg",RespBeanEnum.REPEAT_ERROR.getMessage());
            System.out.println("重复抢购");
            return "seckillFail";
        }
        System.out.println("没有重复抢购");

        Order order = orderService.seckill(user,goods);
        model.addAttribute("order",order);
        model.addAttribute("goods",goods);

        System.out.println("do seckill");
        return "orderDetail";
    }

    /**
     * 秒杀
     * windows（30000样本）
     *              优化前QPS：449.5
     *      页面缓存优化后 QPS：523.0
     *
     *
     *
     * linux优化前QPS：165.9 （30000样本）
     * @param path
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value="/{path}/doSeckill",method= RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(@PathVariable String path, User user, long goodsId){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        ValueOperations valueOperations = redisTemplate.opsForValue();
        boolean check = orderService.checkPath(user,goodsId,path);
        if(!check){
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }

        //判断是都重复抢购
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue()
                .get("order:" + user.getId() + ":" + goodsId);
        if(seckillOrder!= null){
            System.out.println("重复抢购");
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        //内存标记，减少Redis的访问
        if(emptyStockMap.get(goodsId)){
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //预减库存
//        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId),
                Collections.EMPTY_LIST);
        if(stock < 0){
            emptyStockMap.put(goodsId,true);
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        SeckillMessage seckillMessage = new SeckillMessage(user,goodsId);
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
        //前端接收0时，就会展示排队中
        return RespBean.success(0);


        /*
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);

        //判断库存
        if(goods.getStockCount() <= 0){
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        //判断是都重复抢购
//        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>()
//                .eq("user_id", user.getId()).eq("goods_id", goodsId));
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue()
                .get("order:" + user.getId() + ":" + goods.getId());
        if(seckillOrder!= null){
            System.out.println("重复抢购");
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        System.out.println("没有重复抢购");

        Order order = orderService.seckill(user,goods);


        System.out.println("do seckill");
        return RespBean.success(order);

         */
    }

    /**
     * 获取秒杀结果
     * @param user
     * @param goodsId
     * @return orderId: 秒杀成功，-1 - 秒杀失败，0 - 排队中
     */
    @RequestMapping(value="/result",method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user,Long goodsId){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        long orderId = seckillOrderService.getResult(user,goodsId);
        return RespBean.success(orderId);
    }

    /**
     * 获取秒杀地址
     * @param user
     * @param goodsId
     * @return
     */
    @AccessLimit(second=5,maxCount=5,needLogin=true)
    @RequestMapping(value = "/path",method = RequestMethod.GET)
    @ResponseBody
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        //限制访问次数，5秒内访问5次
//        String uri = request.getRequestURI();
//        captcha = "0";
//        Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
//        if(count == null){
//            valueOperations.set(uri + ":" + user.getId(), 1, 5, TimeUnit.SECONDS);
//        }else if(count < 5){
//            valueOperations.increment(uri + ":" + user.getId());
//        }else{
//            return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
//        }

        boolean check = orderService.checkCaptcha(user,goodsId,captcha);
        if(!check){
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }
        String str = orderService.createPath(user,goodsId);
        return RespBean.success(str);
    }

    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public void verifyCode(User user, Long goodsId, HttpServletResponse response){
        System.out.print("first mark");

        System.out.println("goodsId:" + goodsId);
        if(user == null || goodsId < 0){
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        System.out.println("user:" + user.toString());
        //设置请求头为输出图片的类型
        response.setContentType("image/jpg");
        response.setHeader("Pargam","No-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0);
        //生成验证码，将结果放入Redis
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130,32,3);
        redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodsId
                ,captcha.text(),300, TimeUnit.SECONDS);
        try{
            System.out.print("!!!!!!!!!before mark");
            captcha.out(response.getOutputStream());
            System.out.print("!!!!!!!!!after mark");
        } catch (IOException e) {
            log.error("验证码生成失败",e.getMessage());
        }

    }


    /**
     * 系统初始化，把库存商品数量加载到Redis
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if(CollectionUtils.isEmpty(list)){
            return;
        }
        list.forEach(goodsVo ->{
            redisTemplate.opsForValue().set("seckillGoods:"+goodsVo.getId(),goodsVo.getStockCount());
            emptyStockMap.put(goodsVo.getId(),false);
        });

    }
}
