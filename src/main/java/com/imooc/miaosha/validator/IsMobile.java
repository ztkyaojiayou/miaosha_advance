package com.imooc.miaosha.validator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * 这里我们自定义一个（利用jsr303框架进行参数校验时，验证是否是手机号的）注解
 * （因为框架没有提供，因此需要自定义，按照源码规范依葫芦画瓢即可，不难）
 * 分三步进行：
 * 1.先定义默认值
 * 2.实现ConstraintValidator接口
 * 3.编写校验手机号是否符合规范的具体代码
 */
//1.先定义默认值
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {IsMobileValidator.class })
public @interface  IsMobile {
	
	boolean required() default true;//默认 必须要输入手机号
	
	String message() default "手机号码格式错误";//默认 若校验不通过，返回的信息：“手机号码格式错误”

	//下面这两个参数不用管，copy源码即可
	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };
}
