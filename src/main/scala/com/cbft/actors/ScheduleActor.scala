package com.cbft.actors

import akka.actor.{Actor, Cancellable, Status}
import akka.event.Logging
import com.cbft.messages.NodeOnline
import com.cbft.messages.ScheduleCancel

import scala.collection.mutable

/*
* 节点启动之后，会使用定时调度程序周期性询问其他节点是否在线
* ScheduleActor 负责在节点启动之后，关闭对该节点进行询问的调度程序
*/

class ScheduleActor(schedules : mutable.HashMap[String,Cancellable]) extends Actor{
  val schedulemap = schedules
  val log = Logging(context.system, this)

  override def receive = {
    case NodeOnline(node: String,boolean: Boolean) =>{
      if(boolean==true){
        val scheduleb = schedulemap.get(node)
        scheduleb match {
          case Some(schedule) => { schedule.cancel()}
          case None => {log.info("received unknown message: schedule")}
        }
      }
    }
    case ScheduleCancel(node: String,schedule: Cancellable) =>{
      schedulemap.put(node,schedule)
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
