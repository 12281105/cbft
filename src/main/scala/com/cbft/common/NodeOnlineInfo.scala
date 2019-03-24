package com.cbft.common

import scala.collection.mutable.HashMap

class NodeOnlineInfo private{
  var online_nodes = new HashMap[String,Boolean]()
  var online_size : Int = 0
  val lock = ""

  private def addNodeState_(nodeName:String,state:Boolean): Unit ={
    lock.synchronized {
      online_nodes.put(nodeName,state)
      online_size = online_nodes.filter(entry => entry._2).size
    }
  }

  private def getNodeState_(nodeName:String): Boolean={
    return online_nodes.getOrElse(nodeName,false)
  }

  private def onlineNodeSize_(): Int = {
    lock.synchronized {
      return online_size
    }
  }
}

object NodeOnlineInfo{
  val nodeOnlineInfo = new NodeOnlineInfo

  def addNodeState(nodeName:String,state:Boolean): Unit ={
    nodeOnlineInfo.addNodeState_(nodeName,state)
  }

  def getNodeState(nodeName:String): Boolean={
    nodeOnlineInfo.getNodeState_(nodeName)
  }

  def onlineNodeSize(): Int = {
    nodeOnlineInfo.onlineNodeSize_()
  }

}
