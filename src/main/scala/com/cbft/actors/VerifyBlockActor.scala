package com.cbft.actors

import java.util.Date

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.common.{NodeInfo, ViewInfo}
import com.cbft.messages.{Block, BlockVote, CommonHashSet, VerifyBlock}
import com.cbft.utils.BroadcastUtil
import com.roundeights.hasher.Implicits._

import scala.collection.mutable.{HashMap, LinkedHashSet}

class VerifyBlockActor extends Actor{
  val commonHashtable = new HashMap[String,LinkedHashSet[String]]
  val blocktables = new HashMap[String,VerifyBlock]
  val log = Logging(context.system, this)

  override def receive = {
    //对于相同batch，我们基本认为commonHashSet消息先到，VerifyBlock消息后到
    case verifyBlock : VerifyBlock => {
      //验证块，如果为空块，直接投反对票
      //验证块中的每个request，commonHashSet是否包含该requestHash，以及交易request.hash256 == requestHash
      if(ViewInfo.getPrimaryNode().equals(verifyBlock.node)){
        var verifyResult : Boolean = true
        val commonHashSet = commonHashtable.getOrElse(verifyBlock.batchnum,null)
        if(commonHashSet!=null){
          if(verifyBlock.block.requests == null){
            verifyResult = false
          }
          else{
            verifyBlock.block.requests.foreach(tuple => {
              if(verifyResult && commonHashSet.contains(tuple._1) && tuple._1.equals(tuple._2.sha256.hex)){}
              else {
                verifyResult = false
                //log.warning("Verify Block Failed (Batchnum = {})",verifyBlock.batchnum)
              }
            })
          }
          //向其他节点转发投票结果
          //log.info("batchnum:{} ------> verifyResult:{}",verifyBlock.batchnum,verifyResult)
          BroadcastUtil.BroadcastMessage(BlockVote(NodeInfo.getHostName(),verifyBlock.batchnum,verifyResult,""))
          commonHashtable.remove(verifyBlock.batchnum)
        }
        else { //这里暂且未考虑这种情况：先收到VerifyBlock，后收到CommonHashSet（似乎不可能发生,但是实际上发生了）
          blocktables.put(verifyBlock.batchnum,verifyBlock)
          //log.error("VerifyBlockActor receive message VerifyBlock before message CommonHashSet For the same batchnum:{} --> {}",verifyBlock.batchnum,commonHashtable)
        }
      }
      else{
        log.warning("node({}) receive verifyblock not from primary node",NodeInfo.getHostName())
      }
    }
    case commonHashSet : CommonHashSet => {
      val verifyBlock = blocktables.getOrElse(commonHashSet.batchnum,null)
      if(verifyBlock == null){
        commonHashtable.put(commonHashSet.batchnum,commonHashSet.commonset)
      }
      else{
        if(ViewInfo.getPrimaryNode().equals(verifyBlock.node)){
          var verifyResult : Boolean = true
          val commonSet = commonHashSet.commonset
          if(verifyBlock.block.requests == null){
            verifyResult = false
          }
          else{
            verifyBlock.block.requests.foreach(tuple => {
              if(verifyResult && commonSet.contains(tuple._1) && tuple._1.equals(tuple._2.sha256.hex)){ }//log.warning("Verify Block Failed (Batchnum = {})",verifyBlock.batchnum)
              else { verifyResult = false }
            })
          }
          //向其他节点转发投票结果
          //log.info("batchnum:{} ------> verifyResult:{}",verifyBlock.batchnum,verifyResult)
          BroadcastUtil.BroadcastMessage(BlockVote(NodeInfo.getHostName(),verifyBlock.batchnum,verifyResult,""))
        }
        else{
          log.warning("node({}) receive verifyblock not from primary node",NodeInfo.getHostName())
        }
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
