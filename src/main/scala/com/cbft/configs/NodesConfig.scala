package com.cbft.configs

import scala.collection.mutable.{LinkedHashMap,Map}

class NodesConfig private{
  var nodes = new LinkedHashMap[String,String]

  private def addNode_(nodeName:String,nodeAddress:String): Unit ={
    nodes.put(nodeName,nodeAddress)
  }
  private def getNode_(nodeName:String): String ={
    return nodes.getOrElse(nodeName,null)
  }

  private def getNodes_(): Map[String,String]={
    return nodes
  }

  private def NodeSize_(): Int = {
    return nodes.size
  }

  private def getNodeNames_(): List[String] ={
    return nodes.keys.toList
  }
}

object NodesConfig{
  val nodeConfig = new NodesConfig

  def addNode(nodeName:String,nodeAddress:String): Unit ={
    nodeConfig.addNode_(nodeName,nodeAddress)
  }

  def getNode(nodeName:String): String ={
    return nodeConfig.getNode_(nodeName)
  }

  def getNodes():Map[String,String]={
    return nodeConfig.getNodes_()
  }

  def NodeSize(): Int = {
    return nodeConfig.NodeSize_()
  }

  def getNodeNames(): List[String] = {
    return nodeConfig.getNodeNames_()
  }
}
