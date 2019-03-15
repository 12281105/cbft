package com.cbft.messages

import akka.actor.Cancellable
import spray.json.DefaultJsonProtocol
import akka.dispatch.ControlMessage

import scala.collection.mutable.{HashSet, LinkedHashSet, HashMap}

trait Message {

}

case class ActorRefReady() extends Message

case class SendRequestHashSet(batchnum : String) extends Message with ControlMessage

case class NodeOnline(node: String,boolean: Boolean) extends Message

case class ScheduleCancel(node: String,schedule: Cancellable) extends Message

case class Block(var height : Int,var pre_hash : String,timestamp : String,merkle_root : String,var cur_hash : String,requests : LinkedHashSet[(String,String)])

case class Transaction(txid: String,from : String,to : String,amount : Double,gentime : String,sign : String)

case class Request(rtno : String,tx : Transaction, reqtime : String, client : String) extends Message

case class RequestHashSet(node : String,batchnum : String,requestset : LinkedHashSet[String],sign : String) extends Message

case class CommonHashSet(batchnum : String,commonset : LinkedHashSet[String]) extends Message

case class VerifyBlock(node : String,batchnum : String,block : Block,sign : String) extends Message

case class BlockVote(node : String,batchnum : String,vote : Boolean,sign : String) extends Message

case class BlockVoteSet(node : String,batchnum : String,voteSet: HashSet[BlockVote],sign : String) extends Message

case class RawBlock(node : String,batchnum : String,block : Block,sign : String) extends Message

case class GenesisBlock(node : String,batchnum : String,block : Block,sign : String) extends Message

case class VoteResult(batchnum : String,result : Boolean) extends Message

case class ReadWriteItem(account : String ,change : Double) extends Message

case class ReadWriteSet(newstates : HashMap[String,Double]) extends Message

case class SyncFinish() extends Message

object MessageJsonProtocol extends DefaultJsonProtocol {
  implicit val transactionFormat = jsonFormat6(Transaction)
  implicit val requestFormat = jsonFormat4(Request)
}
