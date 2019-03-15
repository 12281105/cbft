package com.cbft.actors

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.cbft.messages.{Block, ReadWriteItem, ReadWriteSet}
import com.cbft.utils.MysqlUtil

import scala.collection.mutable
import scala.collection.mutable.HashMap

class UpdateStateActor(baseid : String) extends Actor{
  val log = Logging(context.system, this)
  val accountinfo = new HashMap[String,Double]
  val account_baseid = baseid

  {
    MysqlUtil.getBalanceAccounts(account_baseid,accountinfo)
  }


  override def receive = {
    case ReadWriteSet(read_write_set) => {
      val newstates = new HashMap[String,Double]
      read_write_set.foreach(entry => {
        var balance : Double = accountinfo.getOrElse(entry._1,Double.NaN)
        if(balance.isNaN){
          log.info("account {} is not managed by UpdateStateActor",entry._1)
        }else{
          accountinfo.put(entry._1,balance+entry._2)
          newstates.put(entry._1,balance+entry._2)
        }
      })
      val res = MysqlUtil.updateAccounts(newstates)
      if(!res){
        log.info("UpdateStateActor failed to write ReadWriteSet")
      }
    }
    case ReadWriteItem(account,change) => {
      var balance : Double = accountinfo.getOrElse(account,Double.NaN)
      if(balance.isNaN){
        log.info("account {} is not managed by UpdateStateActor",account)
      }else{
        accountinfo.put(account,balance+change)
        val res = MysqlUtil.updateAccounts(HashMap[String,Double]((account,balance+change)))
        if(!res){
          log.info("UpdateStateActor failed to write ReadWriteSet")
        }
      }
    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
