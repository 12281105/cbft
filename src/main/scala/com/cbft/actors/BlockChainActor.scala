package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.common.{NodeInfo, ViewInfo}
import com.cbft.messages._
import com.cbft.utils.MysqlUtil
import com.roundeights.hasher.Implicits._
import scala.concurrent.duration._
import scala.collection.mutable.HashMap

class BlockChainActor extends Actor {
  var curHeight: Int = 0
  var preHash: String = null
  var expectedBatchnum: Int = 0
  val blocks = new HashMap[Int, Block]
  val voteRess = new HashMap[Int, VoteResult]
  val log = Logging(context.system, this)

  {
    //初始化curHeight和preHash
    val blocknum = MysqlUtil.getBlockNumber()
    //如果区块链已经生成了一些区块
    if(blocknum>0){
      curHeight = blocknum
      preHash = MysqlUtil.getCurrentHash(blocknum-1)
    }
  }

  override def receive = {
    case genesisBlock : GenesisBlock => {
      if(ViewInfo.getPrimaryNode().equals(genesisBlock.node) && "0".equals(genesisBlock.batchnum)){
        log.info("COMMIT BLOCK: height = {} block = {}",curHeight,genesisBlock.block)
        context.actorSelection("/user/cbft_storeblock") ! genesisBlock.block
        preHash = genesisBlock.block.cur_hash
        curHeight += 1
      }
    }
    case x : ViewChange =>{
      import context.dispatcher
      context.system.scheduler.scheduleOnce(5 seconds,new Runnable{
        override def run(): Unit = {
          var batchnum_start = expectedBatchnum
          var batchnum_end = expectedBatchnum
          val mlist = voteRess.toSeq.sortBy(_._1)
          if(mlist.size!=0){
            batchnum_end = voteRess.toSeq.sortBy(_._1).apply(0)._1
          }
          println("BlockChainActor >>>>> viewchange occur batchnum_start="+batchnum_start+" and batchnum_end="+batchnum_end)
          context.actorSelection("/user/cbft_buildblock") ! BlockRetrans(batchnum_start,batchnum_end)
        }
      })
    }
    case rawblock : RawBlock => {
      //验证是否是主节点发送的Block 和 签名信息 unimplemented ???
      if(ViewInfo.getPrimaryNode().equals(rawblock.node)){
        blocks.put(rawblock.batchnum.toInt,rawblock.block)
      }
      else{
        log.warning("node({}) receive rawblock not from primary",NodeInfo.getHostName())
      }
    }
    case voteResult : VoteResult => {
      //如果投票结果的batchnum和expectedBatchnum相等
      println("BlockChainActor >>>>> receive voteResult batch ["+voteResult.batchnum+"]")
      if((expectedBatchnum+"").equals(voteResult.batchnum)){
        voteRess.put(voteResult.batchnum.toInt,voteResult)
        var notBreak = true
        //println(voteRess.toSeq.sortBy(_._1))
        voteRess.toSeq.sortBy(_._1).foreach(tuple => {
          if(notBreak && tuple._1.equals(expectedBatchnum)){
            blocks.get(tuple._1) match {
              case Some(block) => {
                if(tuple._2.result){
                  block.pre_hash = preHash
                  block.height = curHeight
                  block.cur_hash = (block.pre_hash+block.merkle_root+block.timestamp).sha256.hex
                  //数据库存储新块，unimplement
                  log.info("COMMIT BLOCK: batch = {}  height = {}  size={}",expectedBatchnum,curHeight,block.requests.size)
                  context.actorSelection("/user/cbft_storeblock") ! block
                  context.actorSelection("/user/cbft_executetransaction") ! block
                  context.actorSelection("/user/cbft_cleanup") ! CleanUp(expectedBatchnum.toString,block.requests.map(entry=>entry._1))
                  context.actorSelection("/user/cbft_buildblock") ! BlockConfirm(expectedBatchnum)
                  curHeight += 1
                  expectedBatchnum += 1
                  preHash = block.cur_hash
                  blocks.remove(tuple._1)
                  voteRess.remove(tuple._1)
                }
                else{
                  log.info("Discard rawblock in batchnum={}",tuple._1)
                  context.actorSelection("/user/cbft_cleanup") ! CleanUp(expectedBatchnum.toString,null)
                  context.actorSelection("/user/cbft_buildblock") ! BlockConfirm(expectedBatchnum)
                  expectedBatchnum += 1
                  blocks.remove(tuple._1)
                  voteRess.remove(tuple._1)
                }
              }
              case None => {
                log.error("BlockChainActor receive message voteResult before message RawBlock For the same batchnum")
              }
            }
          }
          else{
            notBreak = false
          }
        })
      }
      else { //VoteResult不是按序到达的，需要等待，按序提交
        voteRess.put(voteResult.batchnum.toInt,voteResult)
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
