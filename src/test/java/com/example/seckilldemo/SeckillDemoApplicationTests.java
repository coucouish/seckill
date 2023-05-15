package com.example.seckilldemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SeckillDemoApplicationTests {
	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private RedisScript<Boolean> script;

	@Test
	public void testLock01() {
		ValueOperations valueOperations = redisTemplate.opsForValue();
		//占位，如果key不存在才可以设置成功
		Boolean isLock = valueOperations.setIfAbsent("k1","v1");
		if(isLock){
			valueOperations.set("name","coucou");
			String name = ((String) valueOperations.get("name"));
			System.out.println("name = " + name);
			//如果中途抛异常，则中途结束，不会达到删除锁的那一步
//			Integer.parseInt("xxxx");
//
			//操作结束，删除锁
			redisTemplate.delete("k1");
		}else{
			System.out.println("有线程在使用，请稍后再试！");
		}
	}

	@Test
	public void testLock02(){
		ValueOperations valueOperations = redisTemplate.opsForValue();
		//设置一个创建五秒后会被自动销毁的锁
		Boolean isLock = valueOperations.setIfAbsent("k1","v1", 5, TimeUnit.SECONDS);
		if(isLock){
			valueOperations.set("name","coucou");
			String name = ((String) valueOperations.get("name"));
			System.out.println("name = " + name);
			//如果中途抛异常，则中途结束，不会达到删除锁的那一步
			Integer.parseInt("xxxx");

			//操作结束，删除锁
			redisTemplate.delete("k1");
		}else{
			System.out.println("有线程在使用，请稍后再试！");
		}

	}

	@Test
	public void testLock03(){
		ValueOperations valueOperations = redisTemplate.opsForValue();
		String value = UUID.randomUUID().toString();
		Boolean isLock = valueOperations.setIfAbsent("k1",value,5,TimeUnit.SECONDS);
		if(isLock){
			valueOperations.set("name","coucou");
			String name = ((String) valueOperations.get("name"));
			System.out.println("name = " + name);
			System.out.println("random value = " + value);
			System.out.println("value of the key = " + valueOperations.get("k1"));
			Boolean result = ((Boolean) redisTemplate.execute(script, Collections.singletonList("k1"), value));
			System.out.println(result);
		}else{
			System.out.println("有线程在使用，请稍后再试！");
		}

	}

}
