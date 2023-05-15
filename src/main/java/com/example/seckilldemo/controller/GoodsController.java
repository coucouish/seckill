package com.example.seckilldemo.controller;

import com.example.seckilldemo.pojo.User;
import com.example.seckilldemo.service.IGoodsService;
import com.example.seckilldemo.service.IUserService;
import com.example.seckilldemo.vo.DetailVo;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.RespBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

/**
 * 商品
 */
@Controller
@Slf4j
@RequestMapping("/goods")
public class GoodsController {
    @Autowired
    private IUserService userService;

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;
    /**
     * 跳转到商品列表页
     * windows:（30000样本）
     *           优化前QPS: 932.1
     *    页面缓存优化后QPS: 867.6
     *
     *
     * linux:（30000样本）
     *          优化前QPS：843.6
     * @param model
     * @param user
     * @return
     */
    @RequestMapping(value = "/toList",produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toList(Model model, User user,
                         HttpServletRequest request,HttpServletResponse response){
//        if(StringUtils.isEmpty(ticket)){
//            return "login";
//        }
//
////        User user = (User) session.getAttribute(ticket);
//        User user = userService.getUserByCookie(ticket, request, response);
//        if(user == null){
//            return "login";
//        }


        //从redis中获取页面，如果不为空，则直接返回页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = ((String) valueOperations.get("goodsList"));
        if(!StringUtils.isEmpty(html)){
            return html;
        }

        model.addAttribute("user",user);
        model.addAttribute("goodsList",goodsService.findGoodsVo());

        //如果为空，手动渲染（使用ThymeleafViewResolver），存入Redis并返回
        WebContext context = new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", context);
        if(!StringUtils.isEmpty(html)){
            valueOperations.set("goodsList",html,60, TimeUnit.SECONDS);
        }

//        return "goodsList";
        return html;
    }

    /**
     * 跳转商品详情页
     *
     */
    @RequestMapping(value ="/toDetail2/{goodsId}",produces="text/html;charset=utf-8")
    @ResponseBody
    public String toDetail2(Model model, User user,@PathVariable long goodsId,
                           HttpServletRequest request,HttpServletResponse response){
        ValueOperations valueOperations = redisTemplate.opsForValue();

        //从redis中获取页面，如果不为空，则直接返回页面
        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
        if(!StringUtils.isEmpty(html)){
            return html;
        }

        model.addAttribute("user",user);
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date currentDate = new Date();

        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSecondsToStart = 0;
        int remainSecondsToEnd = 0;

        if(currentDate.before(startDate)){
            //秒杀还未开始
            remainSecondsToStart = ((int) ((startDate.getTime() - currentDate.getTime()) / 1000));
            remainSecondsToEnd = ((int) ((endDate.getTime() - currentDate.getTime()) / 1000));
        }else if(currentDate.after(endDate)){
            //秒杀已经结束
            seckillStatus = 2;
            remainSecondsToStart = -1;
        }else{
            //秒杀进行中
            seckillStatus = 1;
            remainSecondsToStart = 0;
            remainSecondsToEnd = ((int) ((endDate.getTime() - currentDate.getTime()) / 1000));
        }
        model.addAttribute("remainSecondsToStart",remainSecondsToStart);
        model.addAttribute("remainSecondsToEnd",remainSecondsToEnd);
        model.addAttribute("seckillStatus",seckillStatus);
        model.addAttribute("goods",goodsVo);

        WebContext context = new WebContext(request,response,
                request.getServletContext(),request.getLocale(),model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail",context);
        if(!StringUtils.isEmpty(html)){
            valueOperations.set("goodsDetail:" + goodsId,html,60,TimeUnit.SECONDS);
        }
//        return "goodsDetail";
        return html;
    }


    /**
     * 跳转商品详情页
     *
     */
    @RequestMapping("/toDetail/{goodsId}")
    @ResponseBody
    public RespBean toDetail(Model model, User user, @PathVariable long goodsId){
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date currentDate = new Date();

        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSecondsToStart = 0;
        int remainSecondsToEnd = 0;

        if(currentDate.before(startDate)){
            //秒杀还未开始
            remainSecondsToStart = ((int) ((startDate.getTime() - currentDate.getTime()) / 1000));
            remainSecondsToEnd = ((int) ((endDate.getTime() - currentDate.getTime()) / 1000));
        }else if(currentDate.after(endDate)){
            //秒杀已经结束
            seckillStatus = 2;
            remainSecondsToStart = -1;
        }else{
            //秒杀进行中
            seckillStatus = 1;
            remainSecondsToStart = 0;
            remainSecondsToEnd = ((int) ((endDate.getTime() - currentDate.getTime()) / 1000));
        }

        DetailVo detailVo = new DetailVo();
        detailVo.setUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setSeckillStatus(seckillStatus);
        detailVo.setRemainSecondsToStart(remainSecondsToStart);
        detailVo.setRemainSecondsToEnd(remainSecondsToEnd);

        return RespBean.success(detailVo);
    }
}
