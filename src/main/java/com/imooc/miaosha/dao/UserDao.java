package com.imooc.miaosha.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.imooc.miaosha.bean.User;

@Mapper
public interface UserDao {

	//根据用户ID（手机号）获取普通用户信息
	@Select("select * from user where id = #{id}")
	public User getById(@Param("id")int id	);

	//插入/添加普通用户的账号密码信息到数据库
	@Insert("insert into user(id, name)values(#{id}, #{name})")
	public int insert(User user);
	
}
