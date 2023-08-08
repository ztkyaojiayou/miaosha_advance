package com.imooc.miaosha.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring4.context.SpringWebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import com.imooc.miaosha.bean.MiaoshaUser;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.LoginService;
import com.imooc.miaosha.vo.GoodsDetailVo;
import com.imooc.miaosha.vo.GoodsVo;
/**
 * 注意：对于SSM框架，每一层的目的其实都是一样的，只不过是把同一个业务逻辑分为三层来做而已
 * 因此，这三层要连在一起看，controller层是接收用户的请求，service层则是具体处理用户请求，dao层则是处理与此业务相关的数据
 * 这样的话，分工越明确，代码逻辑也就越清晰
 */

/**
 * 这是与秒杀商品列表相关的controller
 * <p>
 * 使用了页面缓存技术：即对于用户发过来的请求商品列表的请求，先从缓存中取，若有，则直接返回给客户端
 * 若没有，则先手动渲染，再返回给客户端，同时把这个页面存入到缓存中，以后要是再请求同样的页面时就可以直接使用了
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    LoginService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    @Autowired
    ApplicationContext applicationContext;

    /**
     * 加入缓存技术之前：QPS:1267 load（负载）:15 mysql（数据库负载非常高）
     * 5000 * 10
     * 加入缓存技术之后：QPS:2884（差不多翻了一倍）, load:5 （负载提高了三倍），效果还是很明显的
     */

    //1.先（为用户）获取/跳转到秒杀商品列表信息页（即所有秒杀商品的列表，goods_list.html）
    //采用页面缓存技术（存到Redis中），这种缓存技术一般保存的时间比较短
    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user) {
        //model.addAttribute（）的作用：往前台传数据，可以传对象，可以传List，前端再通过el表达式 ${}可以获取到，
        model.addAttribute("user", user);
        //1.1先获取秒杀商品列表信息页
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        //1.2再把此页面对象传到前端，用于前端获取相关信息
        model.addAttribute("goodsList", goodsList);

        //1.3再返回一个html页面给前端，之前是直接返回/跳转到一个指定的html页面，现在我们使用页面缓存技术
        //return "goods_list";//这是原始做法，不使用页面缓存技术，由springboot给我们渲染
        //1.3.1先从缓存中取，若存在，则直接返回给客户端/跳转到此页面
        String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        //1.3.2否则，若不存在，则调用thymeleaf模板中的thymeleafViewResolver对我们的goods_list模板进行手动渲染（这个具体过细节了解即可）
        SpringWebContext ctx = new SpringWebContext(request, response,
                request.getServletContext(), request.getLocale(), model.asMap(), applicationContext);
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);//渲染goods_list
        if (!StringUtils.isEmpty(html)) {
            //（1）渲染完了之后，存入缓存中，则之后再请求此页面时就可以直接从缓存中取了
            redisService.set(GoodsKey.getGoodsList, "", html);
        }
        //（2）最后返回手动渲染完的html页面给前端即可（即跳转到秒杀商品列表页面（即goods_list.html））
        return html;
    }

    //2.再根据goodsId（为用户)跳转到某个具体秒杀商品的详情页(goods_detail.html）

    /**
     * 也使用了页面缓存技术，只是由于具体的商品页面与商品有关，随商品ID的改变而改变，因此也叫url缓存技术
     *
     * @param request
     * @param response
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/to_detail/{goodsId}", produces = "text/html")
    @ResponseBody
    public String detail01(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user,
                           @PathVariable("goodsId") long goodsId) {
        //2.1把用户信息传到前端，用于判断用户是否已登录
        model.addAttribute("user", user);
        //2.2把商品信息GoodsVo对象也传到前端，因为前端页面有些地方需要用到
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods", goods);
        //2.3获取秒杀的开始时间、结束时间和当前时间，用于秒杀按钮的控制
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;//表示秒杀还未开始
        int remainSeconds = 0;//表示距离秒杀的倒计时时间
        if (now < startAt) {//秒杀还没开始，开始倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        } else if (now > endAt) {//秒杀已经结束
            miaoshaStatus = 2;//表示秒杀已经结束
            remainSeconds = -1;//把其置为-1
        } else {//秒杀进行中
            miaoshaStatus = 1;//表示秒杀进行中
            remainSeconds = 0;//把其置为0
        }
        //把秒杀倒计时和秒杀状态传到前端
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);

        //2.4再返回一个html页面给前端，之前是直接返回/跳转到一个指定的html页面，现在我们使用页面缓存技术
        //return "goods_detail";//这就是原始做法
        //2.4.1先从Redis缓存中获取，若存在，则直接返回给客户端/跳转到此页面
        String html = redisService.get(GoodsKey.getGoodsDetail, "" + goodsId, String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        //2.4.2若没有，则手动渲染
        SpringWebContext ctx = new SpringWebContext(request, response,
                request.getServletContext(), request.getLocale(), model.asMap(), applicationContext);
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail", ctx);
        //2.4.3把渲染之后的页面存入到缓存中，以后就可以直接从缓存中取
        if (!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKey.getGoodsDetail, "" + goodsId, html);
        }
        //2.4.4最后，把此页面返回给客户端
        return html;
    }

    //最终方案：不使用上述的页面缓存技术，而是使用页面静态化（即不使用thymeleaf+template模板进行渲染了，而是把页面直接放到浏览器上，页面资源放在了static文件夹中）
    @RequestMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail02(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user,
                                          @PathVariable("goodsId") long goodsId) {
        //根据用户选择的goodsId获取此秒杀商品的具体详情页
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        //控制秒杀按钮
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();
        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if (now < startAt) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        } else if (now > endAt) {//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        //最终返回的是商品详情对象vo，用于把页面上的动态信息/字段填充到详情页（即static中的goods_details.htm页面），再展示出来即可
        //而不需要从后端跳转到商品详情页，是直接从前端跳转，我们只需要把页面上那些动态数据填充上去即可（现在基本都是采取这种方案啦）
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        return Result.success(vo);
    }


}
