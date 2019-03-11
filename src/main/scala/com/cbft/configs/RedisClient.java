package com.cbft.configs;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {
    private static JedisPool jedisPool;

    static {
        JedisPoolConfig jedisPoolConfig = initPoolConfig();
        int timeout = 60000;
        jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6379, timeout, "123");
    }

    public static Jedis getJedis()
    {
        return jedisPool.getResource();
    }

    public static void returnJedis(Jedis jedis){
        jedis.close();
    }

    /**
     *   初始化JedisConf
     */
    private static JedisPoolConfig initPoolConfig()
    {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 控制一个pool最多有多少个状态为idle的jedis实例
        jedisPoolConfig.setMaxTotal(500);
        jedisPoolConfig.setMaxIdle(100);
        // 超时时间
        jedisPoolConfig.setMaxWaitMillis(1000);
        // 在borrow一个jedis实例时，是否提前进行alidate操作；如果为true，则得到的jedis实例均是可用的；
        jedisPoolConfig.setTestOnBorrow(true);
        // 在还会给pool时，是否提前进行validate操作
        jedisPoolConfig.setTestOnReturn(true);

        return jedisPoolConfig;
    }
}
