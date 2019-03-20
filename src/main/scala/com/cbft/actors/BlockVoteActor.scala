package com.cbft.actors

import akka.actor.{Actor, Cancellable, Status}
import akka.event.Logging
import com.cbft.common.{NodeInfo, NodeOnlineInfo}
import com.cbft.configs.NodesConfig
import com.cbft.messages.{BlockVote, BlockVoteSet}
import com.cbft.utils.BroadcastUtil

import scala.concurrent.duration._
import scala.collection.mutable.{HashMap, HashSet}

class BlockVoteActor extends Actor{
  val blockVotes = new HashMap[String,HashSet[BlockVote]]
  val schedule_tables = new HashMap[String,Cancellable]()
  val log = Logging(context.system, this)
  import context.dispatcher

  override def receive = {
    case blockVote : BlockVote => {
      var votes = blockVotes.getOrElse(blockVote.batchnum,null)
      if(votes==null){
        votes = HashSet(blockVote)
        blockVotes.put(blockVote.batchnum,votes)
        var schedule: Option[Cancellable] = None
        schedule = Some(context.system.scheduler.schedule(10 seconds,10 seconds,new Runnable{
          val batchnum = blockVote.batchnum
          var count = 0
          override def run(): Unit = {
            var votes = blockVotes.getOrElse(batchnum,null)
            if(votes!=null && votes.size >= NodeOnlineInfo.onlineNodeSize()){
              BroadcastUtil.BroadcastMessage(BlockVoteSet(NodeInfo.getHostName(),batchnum,votes,""))
              blockVotes.remove(batchnum)
              schedule_tables.remove(batchnum)
              schedule.foreach(_.cancel())
            }
            else{
              count+=1
              if(count>=3){
                println("BlockVoteActor >>>>> scheduler for batch ["+batchnum+"] timeout(30s)")
                blockVotes.remove(batchnum)
                schedule_tables.remove(batchnum)
                schedule.foreach(_.cancel())
              }
            }
          }
        }))
        schedule_tables.put(blockVote.batchnum,schedule.get)
      }
      else{
        votes.add(blockVote)
      }
      //对于每个batch，收集到每个节点的投票结果之后，进行第二轮投票
      if(votes.size >= NodeOnlineInfo.onlineNodeSize()){
        BroadcastUtil.BroadcastMessage(BlockVoteSet(NodeInfo.getHostName(),blockVote.batchnum,votes,""))
        blockVotes.remove(blockVote.batchnum)
        val cancellable_opt = schedule_tables.remove(blockVote.batchnum)
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
