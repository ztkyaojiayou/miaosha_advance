package com.imooc.miaosha.exception;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;

/**
 * （先接管异常信息）这是一个我们自定义的全局异常处理器，是给用户看的
 * 目的：用于当出现异常时，给用户返回友好信息，而不是系统所展示的真实的生硬的异常信息
 * （当用户的账号密码不符合规范时，则先交由此异常处理器处理）
 */

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
	@ExceptionHandler(value=Exception.class)
	public Result<String> exceptionHandler(HttpServletRequest request, Exception e){
		e.printStackTrace();
		//明确：异常有很多种的，要分类讨论
		//1.若为我们自定义的GlobalException异常(重点关注）
		if(e instanceof GlobalException) {
			GlobalException ex = (GlobalException)e;
			//则调用GlobalException类中的getCm()方法，输出我们自定义的异常信息
			return Result.error(ex.getCm());
		}else if(e instanceof BindException) {//2.若为绑定异常(系统异常）
			BindException ex = (BindException)e;
			List<ObjectError> errors = ex.getAllErrors();
			ObjectError error = errors.get(0);
			String msg = error.getDefaultMessage();
			return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));//输出“参数校验异常”（这个信息倒是我们自己定义的）
		}else {//3.对于其他异常，则一概输出为“服务器错误”（（这个信息也是我们自己定义的）
			return Result.error(CodeMsg.SERVER_ERROR);
		}
	}
}
