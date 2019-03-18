package com.cbft.utils

import com.cbft.configs.RedisClient
import scala.collection.JavaConverters._

object RedisUtil {
  def HashSet(key1 : String,key2 : String,value : String): Unit ={
    val jedis = RedisClient.getJedis
    jedis.hset(key1,key2,value)
    RedisClient.returnJedis(jedis)
  }
  def GetRequests(key1 : String): Map[String,String] ={
    val jedis = RedisClient.getJedis
    val res = jedis.hgetAll(key1)
    RedisClient.returnJedis(jedis)
    return res.asScala.toMap
  }
  def DeleteKey(key1 : String): Unit = {
    val jedis = RedisClient.getJedis
    jedis.del(key1)
    RedisClient.returnJedis(jedis)
  }
}
