package com.cbft

import java.util.Date

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import akka.event.Logging
import akka.util.Timeout
import com.cbft.AppMain0.system
import com.cbft.common.{NodeInfo, NodesActorRef}
import com.cbft.configs.{NodesConfig, RedisClient}
import com.roundeights.hasher.Implicits._

import scala.language.postfixOps
import com.cbft.messages.{ActorRefReady, Request}
import spray.json._
import com.cbft.messages.MessageJsonProtocol._
import com.cbft.utils.MerkleTree
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.collection.mutable.{HashMap, LinkedHashSet}
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

object TestMain extends App{
  val system = ActorSystem("cbfttest",ConfigFactory.load("nodetest.conf"))
  //println(system.dispatchers.defaultDispatcherConfig)
  //加载配置文件
  //println(system.settings.config.getConfig("cbft.node"))
  import system.dispatcher
  implicit val timeout = Timeout(5 seconds)
  var nodemap = new HashMap[String,String]
  val nodes = system.settings.config.getConfig("cbft.node").entrySet().asScala.foreach(entry => {
    nodemap.put(entry.getKey,entry.getValue.unwrapped().toString)
  })
  val localhost = system.settings.config.getString("cbft.hostname")
  val actorRefMap = new HashMap[String,ActorRef]
  nodemap.foreach(tuple => {
    val address = tuple._2
    val actorRefFuture : Future[ActorRef] = system.actorSelection(s"akka.tcp://cbft@$address/user/cbft_request").resolveOne()
    actorRefFuture.onSuccess({
      case actorRef : ActorRef =>
        this.synchronized{
          actorRefMap.put(tuple._1,actorRef)
        }
    })
    actorRefFuture.onFailure({
      case t => println("ActorRefActor throw exception : {}",t)
    })
  })
  Thread.sleep(5000)
  for(i <- 1 to 100000){
    val now = new Date()
    val nowtimestamp = now.getTime.toString.substring(0,10)
    actorRefMap.foreach(tuple => {
      tuple._2 ! Request(i.toString,"add",nowtimestamp,localhost,"")
    })
    if(i%1000==0){
      Thread.sleep(100)
    }
  }
  /*
  def hash256(origin : String ): String ={
    origin.sha256.hex
  }

  def printlhs(lhs : LinkedHashSet[String]): Unit ={
    println(lhs)
  }

  val now = new Date()
  val nowtimestamp = now.getTime.toString.substring(0,10)
  println(nowtimestamp)
  val request = Request("A->B:50",nowtimestamp,"zhangchi","123456")
  println(request.toString.sha256.hex)
  println(request)
  println(request.toString)
  println(request.toJson)
  println(request.toJson.compactPrint)
  println(request.toJson.prettyPrint)
  println(request.toJson.convertTo[Request])

  val jedisClient = RedisClient.getJedis
  jedisClient.set("request",request.toJson.toString)
  RedisClient.returnJedis(jedisClient)

  val lhs = new LinkedHashSet[String]
  lhs.add("123")
  lhs.add("234")
  lhs.add("345")
  lhs.add("456")
  val slhs = lhs.slice(0,-1)
  val clhs = lhs.clone()
  println(lhs)
  printlhs(lhs)
  println(slhs)
  lhs.clear()
  printlhs(clhs)
  println(slhs)

  val twomap = new HashMap[String,HashMap[String,String]]
  twomap.put("1",HashMap[String,String]("hello1"->"world1","hello2"->"world2"))
  println(twomap)
  val tempmap = twomap.getOrElse("1",null)
  if(tempmap==null){
    twomap.put("1",HashMap[String,String]("hello1"->"world1"))
  }
  else{
    tempmap.put("hello3","world3")
    twomap.put("1",tempmap)
  }
  println(twomap)

  val hashSetMap = new HashMap[String,LinkedHashSet[String]]
  hashSetMap.put("1",LinkedHashSet("1","2","3","4"))
  hashSetMap.put("2",LinkedHashSet("3","4","5","6"))
  hashSetMap.put("3",LinkedHashSet("2","3","4","5"))
  hashSetMap.put("4",LinkedHashSet("1","2","5","6"))
  val res = hashSetMap.values.reduce(_ & _)
  println(res)

  val blocks = LinkedHashSet("hello")//,"world","nihao")
  val tree = MerkleTree(blocks.toList, hash256)
  println(tree)

  val hashlist = LinkedHashSet("1","2","3","5")
  val hashmap = Map("1"->"hello","2"->"world","3"->"nihao","4"->"shijie")
  /*
  val res1 = hashlist.map(hash => hashmap.get(hash) match {
    case Some(s) => s
    case None =>
  })
  */
  val res1 = hashlist.map(hash => {
    val value = hashmap.getOrElse(hash,null)
    if(value != null){
      hash->value
    }
    else{
      hash->null
    }
  })
  println(res1)
  val res2 = res1.filter(key => key._2!=null)
  println(res2)

  val s1 = "zhangchi"
  val s2 = "zhangchi"

  println(s1==s2)

  val map1 = HashMap(21->"hello",1->"world",10->"nihao",3->"shijie")
  val sortmap = map1.toSeq.sortBy(_._1)
  println(sortmap)
  */

}