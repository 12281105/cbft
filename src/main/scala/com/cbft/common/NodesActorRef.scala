package com.cbft.common

import akka.actor.ActorRef

import scala.collection.mutable
import scala.collection.mutable.HashMap

class NodesActorRef private{
  val actorRefs = new HashMap[String,HashMap[String,ActorRef]];

  private def addNodesActorRef_(actorType : String,actorRefMap : HashMap[String,ActorRef]): Unit ={
    actorRefs.put(actorType,actorRefMap)
  }

  private def addNodeActorRef_(actorType : String,nodeName : String,actorRef: ActorRef): Unit ={
    if(actorRefs.getOrElse(actorType,null)==null){
      actorRefs.put(actorType,new HashMap[String,ActorRef])
      actorRefs.get(actorType).get.put(nodeName,actorRef)
    }
    else{
      actorRefs.get(actorType).get.put(nodeName,actorRef)
    }
  }

  private def getNodesActorRef_(actorType : String): HashMap[String,ActorRef] ={
    return  actorRefs.getOrElse(actorType,null)
  }

  private def getNodeActorRef_(actorType : String,nodeName : String): ActorRef ={
    return actorRefs.getOrElse(actorType,null).getOrElse(nodeName,null)
  }
}

object NodesActorRef{
  val nodesActorRef = new NodesActorRef

  def addNodesActorRef(actorType : String,actorRefMap : HashMap[String,ActorRef]): Unit ={
    nodesActorRef.addNodesActorRef_(actorType,actorRefMap)
  }

  def addNodeActorRef(actorType : String,nodeName : String,actorRef: ActorRef): Unit ={
    nodesActorRef.addNodeActorRef_(actorType,nodeName,actorRef)
  }

  def getNodesActorRef(actorType : String): HashMap[String,ActorRef] ={
    return nodesActorRef.getNodesActorRef_(actorType)
  }

  def getNodeActorRef(actorType : String,nodeName : String): ActorRef ={
    return nodesActorRef.getNodeActorRef_(actorType,nodeName)
  }
}
