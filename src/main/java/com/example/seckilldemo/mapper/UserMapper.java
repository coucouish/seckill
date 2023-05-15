package com.example.seckilldemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckilldemo.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Tessa
 * @since 2023-04-23
 */
@Repository(value="userMapper")
public interface UserMapper extends BaseMapper<User> {

}
