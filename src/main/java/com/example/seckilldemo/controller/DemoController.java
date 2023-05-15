package com.example.seckilldemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Author Tessa Z.
 * @Date 2023/4/18 11:47
 * @Description This is a test of controller.
 * @Since version-1.0
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

    /**
     * @Author Tessa Z.
     * @Date 2023/4/18 12:29
     * @Description 测试页面跳转
     * @Param [model]
     * @Return java.lang.String
     * @Since version-1.0
     */
    @RequestMapping("/test")
    public String test(Model model){
        model.addAttribute("name","Vivi");
        return "test";
    }
}
