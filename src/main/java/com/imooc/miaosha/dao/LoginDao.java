package com.imooc.miaosha.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imooc.miaosha.bean.MiaoshaUser;

/**
 * 这是与用户登录相关的DAO
 */

@Mapper
public interface LoginDao {
	//1.根据用户ID（这里就是用户输入的手机号）查找用户名
	@Select("select * from miaosha_user where id = #{id}")
	public MiaoshaUser getById(@Param("id")long id);

	//2.根据用户ID（这里就是用户输入的手机号）更新用户的密码
	@Update("update miaosha_user set password = #{password} where id = #{id}")
	public void update(MiaoshaUser toBeUpdate);
}
