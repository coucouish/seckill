package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.pojo.User;
import com.example.seckilldemo.vo.LoginVo;
import com.example.seckilldemo.vo.RespBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Tessa
 * @since 2023-04-23
 */
@Service
@Primary
public interface IUserService extends IService<User> {

    /**
     * 登录
     *
     * @param loginVo
     * @param request
     * @param response
     * @return
     */
    RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);

    /**
     * 根据cookie获取用户
     */
    User getUserByCookie(String userTicket,HttpServletRequest request,HttpServletResponse response);

    /**
     * 更新密码!!!!
     * @param userTicket
     * @param password
     * @param request
     * @param response
     * @return
     */
    RespBean updatePassword(String userTicket,String password,HttpServletRequest request,HttpServletResponse response);
}
