package com.cbft.common

class ViewInfo private (view : Int){
  var viewnum : Int = view
  var primaryNode : String = null

  private def viewInc_(): Unit ={
    viewnum += 1
  }

  private def getView_(): Int ={
    return viewnum
  }

  private def setPrimaryNode_(node : String): Unit = {
    primaryNode = node
  }

  private def getPrimaryNode_(): String = {
    return primaryNode
  }
}

object ViewInfo{
  val viewInfo = new ViewInfo(0)

  def viewInc(): Unit ={
    viewInfo.viewInc_()
  }

  def getView():Int = {
    return viewInfo.getView_()
  }

  def setPrimaryNode(node : String): Unit = {
    viewInfo.setPrimaryNode_(node)
  }

  def getPrimaryNode(): String = {
    viewInfo.getPrimaryNode_()
  }
}