package com.cbft.actors

import akka.actor.{Actor, Cancellable, Status}
import akka.event.Logging
import com.cbft.common.NodeOnlineInfo
import com.cbft.configs.NodesConfig
import com.cbft.messages.{BlockVoteSet, VoteResult}

import scala.concurrent.duration._
import scala.collection.mutable.{HashMap, HashSet}

class BlockVoteSetActor extends Actor{
  val blockVoteSets = new HashMap[String,HashSet[BlockVoteSet]]
  val schedule_tables = new HashMap[String,Cancellable]()
  val log = Logging(context.system, this)
  import context.dispatcher

  override def receive = {
    case blockVoteSet : BlockVoteSet =>{
      var votesets = blockVoteSets.getOrElse(blockVoteSet.batchnum,null)
      if(votesets==null){
        votesets = HashSet(blockVoteSet)
        blockVoteSets.put(blockVoteSet.batchnum,votesets)
        var schedule: Option[Cancellable] = None
        schedule = Some(context.system.scheduler.schedule(5 seconds,5 seconds,new Runnable{
          val batchnum = blockVoteSet.batchnum
          var count = 0
          override def run(): Unit = {
            var votesets = blockVoteSets.getOrElse(batchnum,null)
            if(votesets!=null && votesets.size >= NodeOnlineInfo.onlineNodeSize()){
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
              val onlinesize = NodeOnlineInfo.onlineNodeSize()
              votestatic.foreach(tuple => {
                if(tuple._2 >= onlinesize){
                  voteResult += 1
                }
                else if(tuple._2 == 0){

                }
                else{
                  log.warning("there is a traitor node(nodename = {})",tuple._1)
                }
              })
              //log.info("batchnum:{} ------> votesets:{}",blockVoteSet.batchnum,votesets)
              //log.info("batchnum:{} ------> voteStatic:{}",blockVoteSet.batchnum,votestatic)
              //log.info("batchnum:{} ------> voteResult:{}",blockVoteSet.batchnum,voteResult)
              if(voteResult >= (2/3.0*NodesConfig.NodeSize())){
                context.actorSelection("/user/cbft_blockchain") ! VoteResult(batchnum,true)
              }
              else{
                context.actorSelection("/user/cbft_blockchain") ! VoteResult(batchnum,false)
              }
              blockVoteSets.remove(batchnum)
              schedule_tables.remove(batchnum)
              schedule.foreach(_.cancel())
            }
            else{
              count+=1
              if(count>=3){
                println("BlockVoteSetActor >>>>> scheduler for batch ["+batchnum+"] timeout(30s)")
                blockVoteSets.remove(batchnum)
                schedule_tables.remove(batchnum)
                schedule.foreach(_.cancel())
                context.actorSelection("/user/cbft_blockchain") ! VoteResult(batchnum,false)
              }
            }
          }
        }))
        schedule_tables.put(blockVoteSet.batchnum,schedule.get)

      }
      else{
        votesets.add(blockVoteSet)
      }
      //统计最后的投票结果
      if(votesets.size >= NodeOnlineInfo.onlineNodeSize()){
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
          if(tuple._2 >= NodeOnlineInfo.onlineNodeSize()){
            voteResult += 1
          }
          else if(tuple._2 == 0){

          }
          else{
            log.warning("there is a traitor node(nodename = {})",tuple._1)
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
        val cancellable_opt = schedule_tables.remove(blockVoteSet.batchnum)
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
