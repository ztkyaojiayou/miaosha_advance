package com.imooc.miaosha.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.imooc.miaosha.dao.LoginDao;
import com.imooc.miaosha.bean.MiaoshaUser;
import com.imooc.miaosha.exception.GlobalException;
import com.imooc.miaosha.redis.MiaoshaUserKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import com.imooc.miaosha.vo.LoginVo;

/**
 * 这是与用户登录相关的controller
 */

@Service
public class LoginService {


    public static final String COOKI_NAME_TOKEN = "token";

    @Autowired
    LoginDao loginDao;

    @Autowired
    RedisService redisService;

    /**
     * 1.用户登录，不直接加入某一个首页，而是先生成一个包含用户信息的token
     *
     * @param response
     * @param loginVo
     * @return 返回一个token
     */
    public String login(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();
        //1.1判断手机号是否存在
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);//当有异常时，则交由我们自定义的全局异常处理器处理
        }
        //1.2验证密码
        String dbPass = user.getPassword();
        String saltDB = user.getSalt();
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
        if (!calcPass.equals(dbPass)) {//当有异常时，则交由我们自定义的全局异常处理器处理
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }

        /**
         * 1.3生成一个包含用户信息的token
         * 这里就是所谓的分布式session共享实现：其实就是单点登录的逻辑，即用一个token当通行证
         */
        //1.3.1通过UUIDUtil工具包生成token（这个随意，只是一般都用这个）
        String token = UUIDUtil.uuid();
        //1.3.2再把此token写入到浏览器的cookie中
        addCookie(response, token, user);
        //1.3.3最后，返回这个token
        return token;
    }

    /**
     * 2.服务端再根据存入Redis的token来获取/确认此用户的信息
     *
     * @param response
     * @param token
     * @return
     */
    public MiaoshaUser getByToken(HttpServletResponse response, String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        MiaoshaUser user = redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
        //延长token的有效期，即再把此token重新写入一次
        if (user != null) {
            addCookie(response, token, user);
        }
        return user;
    }

    /**
     * “把此token写入到浏览器的cookie中”的具体实现（1.3.2）
     *
     * @param response
     * @param token
     * @param user
     */
    private void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {

        //把用户信息以及这个token先写入Redis当中，这样再Redis中就可以根据这个token获取到这个用户的信息啦
        redisService.set(MiaoshaUserKey.token, token, user);

        Cookie cookie = new Cookie(COOKI_NAME_TOKEN, token);
        cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);//把此token写入到浏览器的cookie中
    }

    /**
     * 3.根据用户ID（这里就是用户输入的手机号）来获取用户信息（这也是获取用户信息的一种方式）
     * 使用了对象缓存技术（即只缓存某一个对象，更细腻化，而不是整个页面（一个页面包含很多对象）），缓存用户信息
     * （和页面缓存技术一样，也是存到Redis中）
     *
     * @param id
     * @return
     */
    public MiaoshaUser getById(long id) {
        //3.1先尝试从缓存中查找，若有，则直接使用
        MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, "" + id, MiaoshaUser.class);
        if (user != null) {
            return user;
        }
        //3.2若没有，则再从数据库中查找并返回
        user = loginDao.getById(id);
        //3.3同时，把其存入缓存中，以便下次从缓存中直接获取
        if (user != null) {
            redisService.set(MiaoshaUserKey.getById, "" + id, user);
        }
        return user;
    }

    // http://blog.csdn.net/tTU1EvLDeLFq5btqiK/article/details/78693323
    //4.更新用户的密码，新密码为传入的formPass
    public boolean updatePassword(String token, long id, String formPass) {
        //4.1先通过用户ID（即手机号）获取user
        MiaoshaUser user = getById(id);
        if (user == null) {
            //4.2若数据库中没有，则返回“手机号不存在”
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //4.3再更新数据库，把新密码存入到数据库中
        //注意：既然只更新密码，其他信息不修改的话，应该单独建立一个新对象专门去更新，而不是传入整个user对象，因为有可能会出现安全问题
        MiaoshaUser toBeUpdate = new MiaoshaUser();
        toBeUpdate.setId(id);//ID不变
        toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));//更新密码，同样使用MD5加密
        loginDao.update(toBeUpdate);//写入到数据库
        //4.4同时要同步更新缓存（这一点非常重要，这也是对象缓存和页面缓存最大的区别，如果不同步更新缓存，则有可能二者数据不一致）
        //4.4.1先删除用户ID（手机号）
        redisService.delete(MiaoshaUserKey.getById, "" + id);
        user.setPassword(toBeUpdate.getPassword());
        //4.4.2再更新token（不能删除它，否则会登录失败）
        redisService.set(MiaoshaUserKey.token, token, user);
        return true;
    }

}
