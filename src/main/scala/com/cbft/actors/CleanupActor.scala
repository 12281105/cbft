package com.cbft.actors

import akka.actor.{Actor, Cancellable, Status}
import akka.event.Logging
import com.cbft.common.NodeInfo
import com.cbft.messages.{CleanUp, Request}
import com.cbft.utils.{MysqlUtil, RedisUtil}
import spray.json._
import com.cbft.messages.MessageJsonProtocol._
import scala.concurrent.duration._
import scala.collection.mutable.LinkedHashSet

class CleanupActor extends Actor{
  val log = Logging(context.system, this)
  var return_requests = new LinkedHashSet[Request]
  var schedule : Cancellable = null

  {
    import context.dispatcher
    schedule = context.system.scheduler.schedule(5 seconds,5 seconds,new Runnable {
      override def run(): Unit = {
        this.synchronized {
          val requestnum = return_requests.size
          if (requestnum > 0) {
            println("CleanupActor request size:" + requestnum)
            val send_requests = return_requests.take(requestnum)
            send_requests.foreach(req => {
              context.actorSelection("/user/cbft_request") ! req
            })
            return_requests = return_requests.drop(requestnum)
            println("send request back to RequestActor:" + requestnum)
          }
        }
      }
    })
  }

  override def receive = {
    case clean: CleanUp => {
      //清空Redis缓冲池
      val redis_key = NodeInfo.getHostName()+"_requests_batch_"+clean.batchnum
      val requestmap = RedisUtil.GetRequests(redis_key)
      var req_not_in_block = requestmap
      if(clean.request_hash!=null){
        req_not_in_block = requestmap.--(clean.request_hash)
      }
      this.synchronized {
        req_not_in_block.foreach(entry => return_requests.add(entry._2.parseJson.convertTo[Request]))
      }
      RedisUtil.DeleteKey(redis_key)
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
