package com.example.seckilldemo.controller;

import com.example.seckilldemo.service.IUserService;
import com.example.seckilldemo.vo.LoginVo;
import com.example.seckilldemo.vo.RespBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * @Author: Tessa z.
 * @DateTime: 2023/4/23 17:47
 * @Description: This is a controller for login page
 **/
@Controller
@RequestMapping("/login")
@Slf4j
public class LoginController {

    @Autowired
    private IUserService userService;
    /**
     * 跳转功能
     * @return
     */
    @RequestMapping("/toLogin")
    public String toLogin(){
        return "login";
    }


    /**
     * 登录功能
     * @param loginVo
     * @return
     */
    @RequestMapping("/doLogin")
    @ResponseBody
    public RespBean doLogin(@Valid LoginVo loginVo, HttpServletRequest request, HttpServletResponse response){
        return userService.doLogin(loginVo,request,response);
    }

}
