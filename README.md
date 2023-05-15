# JAVA高并发秒系统
- 项目优化1.0 页面优化
	- 1.添加缓存
		- QPS的一大瓶颈在于对数据库的操作，所以我们可以通过把对数据库的操作数据提取出来，存到缓存中，进而达到优化的目的。
			- 并不是说任何的数据库操作都可以放到缓存，一般我们只会将那种需要频繁被读取并且变更较少的数据放到缓存中，因为做缓存就需要考虑数据一致性的问题。
		- 当用户再次访问页面时，将不再是单纯的执行页面跳转，而是会返回一个完整的页面
		- 缓存的类型：
			- 页面缓存
			- url缓存
			- 对象缓存
		- 用redis做页面缓存
			- 页面缓存
				- 代码示例
					-
					  ```
					  package com.example.seckilldemo.controller;
					  
					  import com.example.seckilldemo.pojo.User;
					  import com.example.seckilldemo.service.IGoodsService;
					  import com.example.seckilldemo.service.IUserService;
					  import com.example.seckilldemo.vo.GoodsVo;
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
					       * windows优化前QPS：932.1 （30000样本）
					       * linux优化前QPS：843.6 （30000样本）
					       * @param model
					       * @param user
					       * @return
					       */
					      @RequestMapping(value = "/toList",produces = "text/html;charset=utf-8")
					      @ResponseBody
					      public String toList(Model model, User user,
					                           HttpServletRequest request,HttpServletResponse response){
					         
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
					              valueOperations.set("goodsList",html,60, TimeUnit.MINUTES);
					          }
					          return html;
					      }
					  }
					  
					  ```
				- 访问页面后，redis数据库的更新结果：新增userTicket信息，以及相应的页面缓存信息
					- ![image.png](../assets/image_1683445435475_0.png)
					- ![image.png](../assets/image_1683445520610_0.png)
					- ![image.png](../assets/image_1683445503163_0.png)
				- 页面缓存的缺陷
					- 虽然我们将页面数据放在redis中做了缓存，加快了页面数据的读取速度，但我们从后端处理完成发送给前端的仍然是整个页面，数据量还是比较大的
					- 对于商品列表，我们一般都会做分页，做页面缓存时一般也只会缓存前面几页（因为大部分用户只会浏览前几页）
			- url缓存
				- url缓存的本质还是页面缓存
					- 当进入一个商品列表后，再通过列表进入不同商品的详情页面的url的goodsId是不一样的，进入的详情页面展示也是不一样的
					- url缓存就是把goodsId不一样的页面（url中的id不一样）同样也做缓存
				- 代码示例
					-
					  ```
					  package com.example.seckilldemo.controller;
					  
					  import com.example.seckilldemo.pojo.User;
					  import com.example.seckilldemo.service.IGoodsService;
					  import com.example.seckilldemo.service.IUserService;
					  import com.example.seckilldemo.vo.GoodsVo;
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
					       * 跳转商品详情页
					       *
					       */
					      @RequestMapping(value ="/toDetail/{goodsId}",produces="text/html;charset=utf-8")
					      @ResponseBody
					      public String toDetail(Model model, User user,@PathVariable long goodsId,
					                             HttpServletRequest request,HttpServletResponse response){
					          ValueOperations valueOperations = redisTemplate.opsForValue();
					  
					          //从redis中获取页面，如果不为空，则直接返回页面
					          //和页面缓存不同的地方，在获取页面信息是加上了goodsId: "goodsDetail:" + goodsId
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
					  
					  		//如果为空，手动渲染（使用ThymeleafViewResolver），存入Redis并返回
					          WebContext context = new WebContext(request,response,
					                  request.getServletContext(),request.getLocale(),model.asMap());
					          html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail",context);
					          if(!StringUtils.isEmpty(html)){
					              valueOperations.set("goodsDetail:" + goodsId,html,60,TimeUnit.SECONDS);
					          }
					  
					          return html;
					      }
					  }
					  ```
				- 访问页面的不同详情页后，redis数据库的更新结果：新增userTicket信息，以及相应的页面缓存信息
					- ![image.png](../assets/image_1683446971036_0.png)
					- ![image.png](../assets/image_1683446985474_0.png)
					- ![image.png](../assets/image_1683447000677_0.png)
					- ![image.png](../assets/image_1683447017309_0.png)
					- ![image.png](../assets/image_1683447070499_0.png)
			- 对象缓存
				- 对象缓存时更加细粒度的缓存，它是针对对象做缓存。
					- 比如在秒杀项目中，对于登录之后的一系列功能，比如展示商品列表页、商品详情页、秒杀等，都会有一个入参User，该User是直接通过Ticket获取的
						- 而为了解决分布式情况下用户不一致问题，我们通过redisTemplate直接将userTicket信息存到redis中了
						- 故在解决用户不一致问题时，也同时对用户对象User进行了缓存（即：对象缓存）。做了该缓存后，后续所有的功能都是从redis中获取的用户信息。
				- 对象缓存需要注意数据一致性问题
					- 对于User对象的缓存，大部分时候时不需要更新的，所以我们的redis缓存中并未对其设置失效时间。但当用户对自身信息做了变更时（如更新密码等，这时数据库的信息会更新），我们就需要及时地更新redis缓存中的相应数据。
					- 更新redis中的对象缓存方法
						- 每次对数据库进行操作时，就把redis内的相关数据清空。然后当用户再次登录时，redis就会获取到最新的用户信息（因为当redis没有缓存数据时，是会直接从数据库中获取数据，然后将最新的数据同步更新到redis中）
					- 代码示例
						-
						  ```
						  package com.example.seckilldemo.service.impl;
						  
						  import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
						  import com.example.seckilldemo.exception.GlobalException;
						  import com.example.seckilldemo.mapper.UserMapper;
						  import com.example.seckilldemo.pojo.User;
						  import com.example.seckilldemo.service.IUserService;
						  import com.example.seckilldemo.utils.CookieUtil;
						  import com.example.seckilldemo.utils.MD5Util;
						  import com.example.seckilldemo.utils.UUIDUtil;
						  import com.example.seckilldemo.vo.LoginVo;
						  import com.example.seckilldemo.vo.RespBean;
						  import com.example.seckilldemo.vo.RespBeanEnum;
						  import org.springframework.beans.factory.annotation.Autowired;
						  import org.springframework.data.redis.core.RedisTemplate;
						  import org.springframework.stereotype.Service;
						  
						  import javax.servlet.http.HttpServletRequest;
						  import javax.servlet.http.HttpServletResponse;
						  
						  /**
						   * <p>
						   *  服务实现类
						   * </p>
						   *
						   * @author Tessa
						   * @since 2023-04-23
						   */
						  @Service
						  public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
						  
						      @Autowired
						      private UserMapper userMapper;
						  
						      @Autowired
						      private RedisTemplate redisTemplate;
						      @Override
						      public RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
						          String mobile = loginVo.getMobile();
						          String password = loginVo.getPassword();
						          
						          //根据手机号获取用户
						          User user = userMapper.selectById(mobile);
						          if(user == null){
						              throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
						          }
						  
						          //判断密码是否正确
						          if (!MD5Util.fromPassToDBPass(password, user.getSalt()).equals(user.getPassword())) {
						              throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
						          }
						  
						          //生成cookie
						          String ticket = UUIDUtil.uuid();
						          //request.getSession().setAttribute(ticket,user);
						  
						          //将用户信息存入redis中
						          redisTemplate.opsForValue().set("user:"+ticket,user);
						          CookieUtil.setCookie(request,response,"userTicket",ticket);
						          return RespBean.success(ticket);
						      }
						  
						      /**
						       * 根据cookie获取用户
						       * @param userTicket
						       * @return
						       */
						      @Override
						      public User getUserByCookie(String userTicket,HttpServletRequest request,HttpServletResponse response) {
						          if(userTicket==null){
						              return null;
						          }
						          User user = (User) redisTemplate.opsForValue().get("user:" + userTicket);
						          if(user != null){
						              CookieUtil.setCookie(request,response,"userTicket",userTicket);
						          }
						          return user;
						      }
						  
						      /**
						       * 更新密码
						       * @param userTicket
						       * @param password
						       * @param request
						       * @param response
						       * @return
						       */
						      @Override
						      public RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response) {
						          User user = getUserByCookie(userTicket, request, response);
						          if(user==null){
						              throw new GlobalException(RespBeanEnum.MOBILE_NOT_EXIST);
						          }
						          user.setPassword(MD5Util.inputPassToDBPass(password,user.getSalt()));
						          int result = userMapper.updateById(user);
						          if(result==1){
						              //删除redis中的相应缓存
						              redisTemplate.delete("user:"+userTicket);
						              //返回成功
						              return RespBean.success();
						          }
						  
						          return RespBean.error(RespBeanEnum.PASSWORD_UPDATE_FAILURE);
						      }
						  }
						  ```
	- 2.页面的静态化，前后端分离
		- 为什么要做页面静态化？
			- 当前秒杀项目只使用了thymeleaf模板，每次浏览器请求时，都要从服务器端获取数据，拼接成模板，渲染，然后返回给浏览器。即使加了缓存，但中间传输时，传输的还是一整个模板引擎。
			- 做了页面静态化后，前端就是html，我们只需要通过服务器端发给前端一些动态的数据，其它基本不会变更的前端元素都是静态的。静态页面获取数据时，不再是通过模板引擎来传输数据、进行页面的跳转，而是在前端进行页面跳转，并通过ajax去请求接口、接收数据并处理数据
		- 优化具体内容
			- 商品详情页面的静态化
				- 将商品详情页面跳转功能换成了创建一个公共返回对象（DetailVo对象），以前通过model传参，改成了通过DetailVo传参。
				- 把thymeleaf模板换成了静态页面，通过静态页面跳转，并通过ajax调用接口并获取数据
			- 秒杀页面的静态化
				- 大致同上
		- 解决商品超卖问题
			- 具体操作
				- 在OrderServiceImpl类中实现扣库存操作时，将原来的直接update处理换成了用sql语句处理，并保留了“操作时库存必须大于零才能操作”的判断条件
					-
					  ```
					  //秒杀商品减库存
					          SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id", goods.getId()));
					          seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
					          
					          //原操作
					  //        seckillGoodsService.updateById(seckillGoods);
					  //        boolean seckillGoodsResult = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>().set("stock_count", seckillGoods.getStockCount())
					  //                .eq("id", seckillGoods.getId()).gt("stock_count", 0));
					  
					  		//修改后
					          boolean seckillGoodsResult = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>().setSql("stock_count = stock_count -1")
					                  .eq("goods_id", goods.getId()).gt("stock_count", 0));
					          if(!seckillGoodsResult){
					              return null;
					          }
					  ```
				- 在秒杀订单数据库（“t_seckill_order”）中，添加索引，通过将用户id和商品id捆绑成一条独有的索引，来确保同一个用户多次秒杀同一个商品的现象不发生
					- ![image.png](../assets/image_1683548120926_0.png)
				- 在秒杀订单SeckillController类中，之前是通过QueryWrapper从数据库中取秒杀订单信息的，以此来判断该用户是否有重复抢购行为，后面将其改为了从redis中读取之前的秒杀订单缓存信息，直接做判断
					- ![image.png](../assets/image_1683548629854_0.png)
					-
					-
	- 3.静态资源的优化
		- 把一些资源（如CSS, JS, 图片等）提前放到另外的地方，并做一些提前的处理
	- 4.[CDN优化](https://juejin.cn/post/7052282275211247646)
		- 什么是CDN？
			- CDN（Content Delivery Network）即内容分发网络，其基本思路是尽可能的避开互联网上有可能影响数据传输速度和稳定性的瓶颈和环节，使内容传输更快、更稳定。
			- CDN是一组分布在多个不同地理位置的Web服务器，用于更加有效地向用户发布内容。
			- CDN系统能够实时的根据网络流量和各节点的连接，负载状况以及用户的距离和响应时间等综合信息将用户的请求重新导向离用户最近的服务节点上。其目的就是使用户能够就近的获取请求数据，解决网络访问拥挤状况，提高用户访问系统的响应时间。
			- CDN将网站的资源发布到离用户最近的网络边缘，用户可以就近取得资源内容。
		- 什么是CDN优化？
			- CDN的本质仍然是一个缓存，而且将数据缓存在里用户最近的地方，使用户以最快的速度获取数据，即网络访问第一跳。
			- 当用户发起内容请求时，不同地区的用户访问同一个域名能得到不同CDN节点的IP地址，这要依赖于CDN服务商提供的智能DNS服务，浏览器发起域名查询时，智能DNS服务会根据用户IP计算并返回离它最近的相同网络运营商的CDN节点IP；
			- 前端需要被加速的文件大致包括js、css、图片、视频等这些文件都是静态的，改动较小，这类静态文件最适合做cdn加速。我们把这些静态文件通过cdn分发到全国乃至世界的各个节点，用户就可以在距离最近的边缘节点拿到所需要的内容，从而提升内容下载速度加快网页打开速度达到性能优化的目的。
