package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.messages.Block
import com.cbft.utils.MysqlUtil

class StoreBlockActor extends Actor{
  val log = Logging(context.system, this)

  override def receive = {
    case block: Block => {
      MysqlUtil.writeBlock(block)
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
