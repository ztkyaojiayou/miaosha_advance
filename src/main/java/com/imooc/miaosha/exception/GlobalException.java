package com.imooc.miaosha.exception;

import com.imooc.miaosha.result.CodeMsg;

//（后具体处理异常信息）这是我们所定义的GlobalException全局异常的具体具体实现
//即：返回我们自定义的异常信息
public class GlobalException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private CodeMsg cm;
	
	public GlobalException(CodeMsg cm) {//构造器
		super(cm.toString());
		this.cm = cm;
	}

	public CodeMsg getCm() {
		return cm;
	}

}
