package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.common.NodeInfo
import com.cbft.configs.NodesConfig
import com.cbft.messages.{BlockVote, BlockVoteSet}
import com.cbft.utils.BroadcastUtil

import scala.collection.mutable.{HashMap, HashSet}

class BlockVoteActor extends Actor{
  val blockVotes = new HashMap[String,HashSet[BlockVote]]
  val log = Logging(context.system, this)

  override def receive = {
    case blockVote : BlockVote => {
      var votes = blockVotes.getOrElse(blockVote.batchnum,null)
      if(votes==null){
        votes = HashSet(blockVote)
        blockVotes.put(blockVote.batchnum,votes)
      }
      else{
        votes.add(blockVote)
      }
      //对于每个batch，收集到每个节点的投票结果之后，进行第二轮投票
      if(votes.size >= NodesConfig.NodeSize()){
        BroadcastUtil.BroadcastMessage(BlockVoteSet(NodeInfo.getHostName(),blockVote.batchnum,votes,""))
        blockVotes.remove(blockVote.batchnum)
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
