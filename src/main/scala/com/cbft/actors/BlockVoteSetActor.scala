package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.configs.NodesConfig
import com.cbft.messages.{BlockVoteSet, VoteResult}

import scala.collection.mutable.{HashMap, HashSet}

class BlockVoteSetActor extends Actor{
  val blockVoteSets = new HashMap[String,HashSet[BlockVoteSet]]
  val log = Logging(context.system, this)

  override def receive = {
    case blockVoteSet : BlockVoteSet =>{
      var votesets = blockVoteSets.getOrElse(blockVoteSet.batchnum,null)
      if(votesets==null){
        votesets = HashSet(blockVoteSet)
        blockVoteSets.put(blockVoteSet.batchnum,votesets)
      }
      else{
        votesets.add(blockVoteSet)
      }
      //统计最后的投票结果
      if(votesets.size >= NodesConfig.NodeSize()){
        var votestatic = new HashMap[String,Int]
        NodesConfig.getNodeNames().foreach(nodename => votestatic.put(nodename,0))
        votesets.foreach(blockVoteSet => {
          blockVoteSet.voteSet.foreach(blockVote => {
            var result = votestatic.getOrElse(blockVote.node,0)
            if(blockVote.vote) result += 1
            votestatic.put(blockVote.node,result)
          })
        })
        var voteResult : Int = 0
        votestatic.foreach(tuple => {
          tuple._2 match {
            case 4 => voteResult += 1
            case 0 => {}
            case _ => log.warning("there is a traitor node(nodename = {})",tuple._1)
          }
        })
        //log.info("batchnum:{} ------> votesets:{}",blockVoteSet.batchnum,votesets)
        //log.info("batchnum:{} ------> voteStatic:{}",blockVoteSet.batchnum,votestatic)
        //log.info("batchnum:{} ------> voteResult:{}",blockVoteSet.batchnum,voteResult)
        if(voteResult >= (2/3.0*NodesConfig.NodeSize())){
          context.actorSelection("/user/cbft_blockchain") ! VoteResult(blockVoteSet.batchnum,true)
          blockVoteSets.remove(blockVoteSet.batchnum)
        }
        else{
          context.actorSelection("/user/cbft_blockchain") ! VoteResult(blockVoteSet.batchnum,false)
          blockVoteSets.remove(blockVoteSet.batchnum)
        }
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
