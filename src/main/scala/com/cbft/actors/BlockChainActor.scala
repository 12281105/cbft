package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.common.{NodeInfo, ViewInfo}
import com.cbft.messages.{Block, GenesisBlock, RawBlock, VoteResult}
import com.roundeights.hasher.Implicits._

import scala.collection.mutable.HashMap

class BlockChainActor extends Actor{
  var curHeight : Int = 0
  var preHash : String = null
  var expectedBatchnum : Int = 0
  val blocks = new HashMap[Int,Block]
  val voteRess = new HashMap[Int,VoteResult]
  val log = Logging(context.system, this)

  override def receive = {
    case genesisBlock : GenesisBlock => {
      if(NodeInfo.getHostName().equals(genesisBlock.node) && "0".equals(genesisBlock.batchnum)){
        log.info("COMMIT BLOCK: height = {} block = {}",curHeight,genesisBlock.block)
        preHash = genesisBlock.block.cur_hash
        curHeight += 1
        expectedBatchnum += 1
      }
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
                  curHeight += 1
                  expectedBatchnum += 1
                  preHash = block.cur_hash
                  blocks.remove(tuple._1)
                  voteRess.remove(tuple._1)
                }
                else{
                  log.info("Discard rawblock in batchnum={}",tuple._1)
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
