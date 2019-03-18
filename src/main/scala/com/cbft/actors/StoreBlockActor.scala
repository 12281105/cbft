package com.cbft.actors

import akka.actor.{Actor, Cancellable, Status}
import akka.event.Logging
import com.cbft.common.{NodeInfo, NodesActorRef}
import com.cbft.messages._
import com.cbft.utils.{BroadcastUtil, MysqlUtil, RedisUtil}
import spray.json._
import com.cbft.messages.MessageJsonProtocol._

import scala.concurrent.duration._
import scala.collection.mutable.LinkedHashSet

class StoreBlockActor extends Actor{
  val log = Logging(context.system, this)

  override def receive = {
    case block: Block => {
      //存储块信息：包括区块头信息和交易信息
      MysqlUtil.writeBlock(block)
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
