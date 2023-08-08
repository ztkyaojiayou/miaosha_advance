package com.imooc.miaosha.validator;
import  javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.imooc.miaosha.util.ValidatorUtil;

//2.再实现ConstraintValidator接口
//ConstraintValidator：这是一个接口，具体的约束验证类需要实现该接口。
//即：这是定义注解时所必需的，我们照做即可
public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

	private boolean required = false;
	
	public void initialize(IsMobile constraintAnnotation) {
		required = constraintAnnotation.required();
	}

	public boolean isValid(String value, ConstraintValidatorContext context) {

		//定义手机号校验规则:不能为空，且要符合手机号的基本规范
		if(required) {//1.若手机号是必须的（因为我们默认就是必须的），则肯定要调用ValidatorUtil（自定义的）工具包进行校验
			return ValidatorUtil.isMobile(value);
		}else {//2.否则，若不是必须的，则即便为空，也没关系，即也返回true
			if(StringUtils.isEmpty(value)) {
				return true;
			}else {//2.1若不是必须的，但不为空，则同样也调用ValidatorUtil（自定义的）工具包进行校验
				return ValidatorUtil.isMobile(value);
			}
		}
	}

}
