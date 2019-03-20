package com.cbft.actors

import akka.actor.{Actor, Cancellable, Status}
import akka.event.Logging
import com.cbft.common.{NodeOnlineInfo}
import scala.concurrent.duration._
import com.cbft.messages.{CommonHashSet, RequestHashSet}
import scala.collection.mutable.{HashMap, LinkedHashSet}

class RequestHashSetActor extends Actor{
  val requestHashtable = new HashMap[String,HashMap[String,LinkedHashSet[String]]]
  val schedule_tables = new HashMap[String,Cancellable]()
  val log = Logging(context.system, this)
  import context.dispatcher

  override def receive = {
    case requestHashSet : RequestHashSet =>{
      //验证requestHashSet签名有效性，unimplement
      //
      var batchRequestHashSet = requestHashtable.getOrElse(requestHashSet.batchnum,null)
      if(batchRequestHashSet == null){
        batchRequestHashSet = HashMap(requestHashSet.node->requestHashSet.requestset)
        requestHashtable.put(requestHashSet.batchnum,batchRequestHashSet)
        var schedule: Option[Cancellable] = None
        schedule = Some(context.system.scheduler.schedule(10 seconds,10 seconds,new Runnable{
          val batchnum = requestHashSet.batchnum
          var count = 0
          override def run(): Unit = {
            var batchRequestHashSet = requestHashtable.getOrElse(batchnum,null)
            if(batchRequestHashSet!=null && batchRequestHashSet.size >= NodeOnlineInfo.onlineNodeSize()){
              val commonHashSet = batchRequestHashSet.values.reduce(_ & _)
              println("RequestHashSetActor >>>>> commonhashset batch ["+batchnum+"] commonhashset size:"+commonHashSet.size)
              //将commonHashSet发送给VerifyBlockActor，供验证阶段使用
              context.actorSelection("/user/cbft_verifyblock") ! CommonHashSet(batchnum,commonHashSet)
              //将commonHashSet发送给BuildBlockActor进行建块处理
              context.actorSelection("/user/cbft_buildblock") ! CommonHashSet(batchnum,commonHashSet)
              //找出公共交集，将requestHashtable中的此表项删除，避免其无限增长
              requestHashtable.remove(batchnum)
              schedule_tables.remove(batchnum)
              schedule.foreach(_.cancel())
            }
            else{
              count+=1
              if(count>=3){
                println("RequestHashSetActor >>>>> scheduler for batch ["+batchnum+"] timeout(30s)")
                requestHashtable.remove(batchnum)
                schedule_tables.remove(batchnum)
                schedule.foreach(_.cancel())
              }
            }
          }
        }))
        schedule_tables.put(requestHashSet.batchnum,schedule.get)
      }
      else{
        batchRequestHashSet.put(requestHashSet.node,requestHashSet.requestset)
      }
      //对于每一个batch，如果本节点收集到足够多的requestHashSet，主节点取sets交集，然后开始建块
      if(batchRequestHashSet.size >= NodeOnlineInfo.onlineNodeSize()){
        val commonHashSet = batchRequestHashSet.values.reduce(_ & _)
        println("RequestHashSetActor >>>>> commonhashset batch ["+requestHashSet.batchnum+"] commonhashset size:"+commonHashSet.size)
        //将commonHashSet发送给VerifyBlockActor，供验证阶段使用
        context.actorSelection("/user/cbft_verifyblock") ! CommonHashSet(requestHashSet.batchnum,commonHashSet)
        //将commonHashSet发送给BuildBlockActor进行建块处理
        context.actorSelection("/user/cbft_buildblock") ! CommonHashSet(requestHashSet.batchnum,commonHashSet)
        //找出公共交集，将requestHashtable中的此表项删除，避免其无限增长
        requestHashtable.remove(requestHashSet.batchnum)
        val cancellable_opt = schedule_tables.remove(requestHashSet.batchnum)
        //取消定时任务
        cancellable_opt.foreach(_.cancel())
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
