package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.common.NodeInfo
import com.cbft.configs.NodesConfig
import com.cbft.messages.{CommonHashSet, RequestHashSet}

import scala.collection.mutable
import scala.collection.mutable.{HashMap, LinkedHashSet}

class RequestHashSetActor extends Actor{
  val requestHashtable = new HashMap[String,HashMap[String,LinkedHashSet[String]]]
  val log = Logging(context.system, this)

  override def receive = {
    case requestHashSet : RequestHashSet =>{
      //验证requestHashSet签名有效性，unimplement

      //
      var batchRequestHashSet = requestHashtable.getOrElse(requestHashSet.batchnum,null)
      if(batchRequestHashSet == null){
        batchRequestHashSet = HashMap(requestHashSet.node->requestHashSet.requestset)
        requestHashtable.put(requestHashSet.batchnum,batchRequestHashSet)
      }
      else{
        batchRequestHashSet.put(requestHashSet.node,requestHashSet.requestset)
      }
      //对于每一个batch，如果本节点收集到足够多的requestHashSet，主节点取sets交集，然后开始建块
      if(batchRequestHashSet.size >= NodesConfig.NodeSize()){
        val commonHashSet = batchRequestHashSet.values.reduce(_ & _)
        println("commonHashSetSize:"+commonHashSet.size)
        //将commonHashSet发送给VerifyBlockActor，供验证阶段使用
        context.actorSelection("/user/cbft_verifyblock") ! CommonHashSet(requestHashSet.batchnum,commonHashSet)
        //将commonHashSet发送给BuildBlockActor进行建块处理
        context.actorSelection("/user/cbft_buildblock") ! CommonHashSet(requestHashSet.batchnum,commonHashSet)
        //找出公共交集，将requestHashtable中的此表项删除，避免其无限增长
        requestHashtable.remove(requestHashSet.batchnum)
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
