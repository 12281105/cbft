package com.cbft.actors

import java.util.Date

import akka.actor.{Actor, Cancellable, Stash, Status}
import akka.dispatch._
import akka.event.Logging
import com.cbft.common.{NodeInfo, NodesActorRef, ViewInfo}
import com.cbft.messages._

import scala.collection.mutable.LinkedHashSet
import scala.concurrent.duration._
import com.roundeights.hasher.Implicits._
import spray.json._
import com.cbft.messages.MessageJsonProtocol._
import com.cbft.utils.{BroadcastUtil, RedisUtil}

import scala.language.postfixOps

class RequestActor extends Actor{
  //requestSet 多个线程共享变量，可能需要特殊处理
  val requestSet = new LinkedHashSet[String]
  val log = Logging(context.system, this)
  //var schedule : Cancellable = null
  //每一秒钟接收到的所有request为一个batch，batchnum和块高度无直接联系
  var batchnum : Int = 1
  //var batchnumredis : Int = 1

  override def receive = {
    case request : Request => {
      log.info("blockchain sync not finish,please wait!")
    }
    case x : SyncFinish =>{
      log.info("blockchain {} sync finish",NodeInfo.getHostName())
      context.become(syncFinish)
      //向其他节点广播发送requestSet,节点签名暂未实现
      /*
      if(NodeInfo.isPrimary()){
        import context.dispatcher
        schedule = context.system.scheduler.schedule(5 seconds,2 seconds,new Runnable {
          override def run(): Unit = {
              BroadcastUtil.BroadcastMessage(SendRequestHashSet(batchnum+""))
              batchnum += 1
          }
        })
      }
      */
    }
    case online : NodeOnline => {
      if(online.boolean==false){
        log.info("{} become offline",online.node)
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }

  def syncFinish : Receive = {
    case request : Request => {
      //log.info("received Request - rtno:{} client:{} operation:{} time:{}",request.rtno,request.client, request.operation, request.time)
      //处理Request消息,Hash request，将交易本身送入redis缓存
      val requestjson = request.toJson.toString
      val requesthash = requestjson.sha256.hex
      requestSet.add(requesthash)
      //RedisClient.getJedis.set()
      RedisUtil.HashSet(NodeInfo.getHostName()+"_requests_batch_"+batchnum,requesthash,requestjson)

      if(requestSet.size >= 100){ //每处理1000个消息，向其他节点发送消息列表
        val sendRequestHashSet = requestSet.clone()
        BroadcastUtil.BroadcastMessage(new RequestHashSet(NodeInfo.getHostName(),batchnum+"",sendRequestHashSet,""))
        batchnum += 1
        requestSet.clear()
      }
    }
    /*
    case sendr : SendRequestHashSet => {
      //可能某些节点的requestSet为空，某些节点的requestSet不为空，对算法的正确运行无影响，为了避免空块的产生（没有交易），会在建块时做特殊处理
      val sendRequestHashSet = requestSet.clone()
      batchnumredis = sendr.batchnum.toInt+1
      BroadcastUtil.BroadcastMessage(new RequestHashSet(NodeInfo.getHostName(),sendr.batchnum,sendRequestHashSet,""))
      requestSet.clear()
    }
    */
    case online : NodeOnline => {
      if(online.boolean==false){
        log.info("{} become offline",online.node)
        context.unbecome()
      }
    }
  }
}
