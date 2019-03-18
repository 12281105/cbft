package com.cbft.actors

import akka.actor.{Actor, ActorRef, Status}
import akka.event.Logging
import akka.util.Timeout
import com.cbft.common.{NodesActorRef, ViewInfo}
import com.cbft.configs.NodesConfig
import com.cbft.messages.{ActorRefReady, NodeOnline}

import scala.concurrent.Future
import scala.concurrent.duration._

class ActorRefActor extends Actor{
  val reftype : List[String] = List("actorref","online","request","requesthashset","verifyblock","blockvote","blockvoteset","blockchain")
  var refcount : Int = 0
  val log = Logging(context.system, this)
  implicit val timeout = Timeout(5 seconds)
  import context.dispatcher

  override def receive = {
    case NodeOnline(node: String,boolean: Boolean) =>{
      if(boolean==true){
        //获取节点上所有Actor的ActorRef（Actor引用）
        reftype.foreach(tp => {
          val address = NodesConfig.getNode(node)
          val actorRefFuture : Future[ActorRef] = context.actorSelection(s"akka.tcp://cbft@$address/user/cbft_$tp").resolveOne()
          actorRefFuture.onSuccess({
            case actorRef : ActorRef =>
              this.synchronized {
                NodesActorRef.addNodeActorRef(tp, node, actorRef)
                refcount += 1
                println(refcount)
                if (refcount == NodesConfig.NodeSize() * reftype.size) { //ActorRefActor完成，向所有节点的onlineActor报告
                  //val primaryNodeAddress = NodesConfig.getNode(ViewInfo.getPrimaryNode())
                  NodesConfig.getNodes().foreach(tuple => {
                    context.actorSelection(s"akka.tcp://cbft@${tuple._2}/user/cbft_online") ! new ActorRefReady()
                  })
                }
              }
          })
          actorRefFuture.onFailure({
            case t => log.info("ActorRefActor throw exception : {}",t)
          })
        })
      }
      else{
        reftype.foreach(tp => {
          NodesActorRef.getNodesActorRef(tp).remove(node)
          refcount -= 1
        })
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
