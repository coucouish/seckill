package com.example.seckilldemo.vo;

import com.example.seckilldemo.utils.ValidatorUtil;
import com.example.seckilldemo.validator.IsMobile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 手机号码校验规则
 */
public class IsMobileValidator implements ConstraintValidator<IsMobile,String> {
    private boolean required = false;
    @Override
    public void initialize(IsMobile constraintAnnotation) {
        //获取关于该字段是否是必填项的信息
        required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if(required){
            return ValidatorUtil.isMobile(s);
        }else{
            if(s.isEmpty()){
                return true;
            }else{
                return ValidatorUtil.isMobile(s);
            }
        }
    }
}
