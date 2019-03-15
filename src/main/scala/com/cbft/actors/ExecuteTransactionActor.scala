package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.messages.{Block, ReadWriteItem, ReadWriteSet, Request}
import spray.json._
import com.cbft.messages.MessageJsonProtocol._

import scala.collection.mutable.HashMap

class ExecuteTransactionActor extends Actor{
  val log = Logging(context.system, this)
  override def receive = {
    case block: Block => {
      val read_write_set = new HashMap[String,Double]
      block.requests.foreach(tuple => {
        val transaction = tuple._2.parseJson.convertTo[Request].tx
        if(transaction.from != transaction.to){
          var from_bal : Double = read_write_set.getOrElse(transaction.from,0)
          var to_bal : Double = read_write_set.getOrElse(transaction.to,0)
          read_write_set.put(transaction.from,from_bal-transaction.amount)
          read_write_set.put(transaction.to,to_bal+transaction.amount)
        }
      })
      /*
      read_write_set.foreach(entry =>{
        context.actorSelection("/user/cbft_updatestate") ! ReadWriteItem(entry._1,entry._2)
      })
      */
      context.actorSelection("/user/cbft_updatestate") ! ReadWriteSet(read_write_set)
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
