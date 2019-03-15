package com.cbft.utils

import com.cbft.common.NodesActorRef
import com.cbft.messages._

object BroadcastUtil {
  def BroadcastMessage(message : Message): Unit ={
    message match {
      case syncFinish : SyncFinish => {
        NodesActorRef.getNodesActorRef("request").foreach(tuple => {
          tuple._2 ! syncFinish
        })
      }
      case online : NodeOnline  => {
        NodesActorRef.getNodesActorRef("request").foreach(tuple => {
          if(tuple._1 != online.node){
            tuple._2 ! online
          }
        })
      }
      case sendRequestHashSet : SendRequestHashSet =>{
        NodesActorRef.getNodesActorRef("request").foreach(tuple => {
          tuple._2 ! sendRequestHashSet
        })
      }
      case requestHashSet : RequestHashSet => {
        NodesActorRef.getNodesActorRef("requesthashset").foreach(tuple =>{
          tuple._2 ! requestHashSet
        })
      }
      case verifyBlock : VerifyBlock => {
        NodesActorRef.getNodesActorRef("verifyblock").foreach(tuple =>{
          tuple._2 ! verifyBlock
        })
      }
      case blockVote : BlockVote => {
        NodesActorRef.getNodesActorRef("blockvote").foreach(tuple =>{
          tuple._2 ! blockVote
        })
      }
      case blockVoteSet : BlockVoteSet => {
        NodesActorRef.getNodesActorRef("blockvoteset").foreach(tuple =>{
          tuple._2 ! blockVoteSet
        })
      }
      case rawBlock : RawBlock =>{
        NodesActorRef.getNodesActorRef("blockchain").foreach(tuple =>{
          tuple._2 ! rawBlock
        })
      }
      case genesisBlock : GenesisBlock =>{
        NodesActorRef.getNodesActorRef("blockchain").foreach(tuple =>{
          tuple._2 ! genesisBlock
        })
      }
    }
  }
}
