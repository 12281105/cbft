package com.cbft.actors

import java.util.Date

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.common.NodeInfo
import com.cbft.messages.{Block, CommonHashSet, RawBlock, VerifyBlock}
import com.cbft.utils.{BroadcastUtil, MerkleTree, RedisUtil}
import com.roundeights.hasher.Implicits._

import scala.collection.mutable.{HashMap, LinkedHashSet}

class BuildBlockActor extends Actor{
  //val commonHashtable = new HashMap[String,LinkedHashSet[String]]
  val log = Logging(context.system, this)

  override def receive = {
    case commonHashSet : CommonHashSet => {
      var block : Block = null
      if(commonHashSet.commonset.size>0){
        //commonHashtable.put(commonHashSet.batchnum,commonHashSet.commonset)
        //开始建块
        val now = new Date()
        val nowtimestamp = now.getTime.toString.substring(0,10)
        val requestHashs = commonHashSet.commonset
        val tree = MerkleTree(requestHashs.toList, hash256)
        //从redis缓存中取交易
        val requestmap = RedisUtil.GetRequests(NodeInfo.getHostName()+"_requests_batch_"+commonHashSet.batchnum)
        println("RequestHashSetActor >>>>> buildblock batch ["+commonHashSet.batchnum+"] request in redis size:"+requestmap.size)
        //当前版本是从四个交易HashSet取交集，所以requests不可能包含null，
        //在下个版本取2/3个节点以上的交集，可能出现主节点没有该交易，而3个从节点中有此交易，造成requests中包含null，这该如何处理？？？
        val requests = requestHashs.map(hash => {
          requestmap.get(hash) match {
            case Some(request) => hash->request
            case None =>
              log.error("batchnum={} CommonHashSet include request not in Redis,Impossible",commonHashSet.batchnum)
              hash->null
          }
        })
        block = new Block(-1,null,nowtimestamp,tree.hash,null,requests)
      }
      else{
        //如果为交易交集为空，建空块
        block = new Block(-1,null,null,null,null,null)
      }
      //println("build block:batchnum={} commonHashset={}",commonHashSet.batchnum,block.requests.size)
      //如果是主节点将新块发送给其他节点验证
      if(NodeInfo.isPrimary()){
        BroadcastUtil.BroadcastMessage(VerifyBlock(NodeInfo.getHostName(),commonHashSet.batchnum,block,""))
        BroadcastUtil.BroadcastMessage(RawBlock(NodeInfo.getHostName(),commonHashSet.batchnum,block,""))
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }

  def hash256(origin : String ): String ={
    origin.sha256.hex
  }
}
