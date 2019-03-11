package com.cbft.actors

import akka.actor.{Actor, Cancellable, Status}
import akka.event.Logging
import com.cbft.messages.NodeOnline

import scala.collection.mutable

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
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
