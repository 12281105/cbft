package com.cbft.common

import com.cbft.configs.NodesConfig

class NodeInfo private{
  var hostname = new String
  var isPrimary : Boolean = false

  private def setHostName_(name : String): Unit ={
    hostname = name
  }

  private def getHostName_(): String ={
    return hostname
  }

  private def isPrimary_(): Boolean = {
    return isPrimary
  }

}

object NodeInfo{
  val nodeInfo = new NodeInfo

  def setHostName(name : String): Unit = {
    nodeInfo.setHostName_(name)
    viewChange()
    println(nodeInfo.isPrimary)
  }

  def viewChange(): Unit = {
    var curView = ViewInfo.getView()
    var nodeNames = NodesConfig.getNodeNames()
    //判断本节点是否为主节点
    nodeInfo.isPrimary = (curView%NodesConfig.NodeSize() == nodeNames.indexOf(nodeInfo.hostname))
    ViewInfo.setPrimaryNode(nodeNames.apply(curView%NodesConfig.NodeSize()))
  }

  def isPrimary(): Boolean = {
    return nodeInfo.isPrimary_()
  }

  def getHostName(): String ={
    return nodeInfo.getHostName_()
  }
}
