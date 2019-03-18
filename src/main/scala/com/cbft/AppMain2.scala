package com.cbft

import akka.actor.{ActorIdentity, ActorRef, ActorSystem, Cancellable, Identify, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.cbft.AppMain0.system
import com.cbft.actors._
import com.cbft.common.NodeInfo
import com.cbft.configs.{MysqlConfig, NodesConfig}
import com.cbft.messages._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.collection.mutable.HashMap
import scala.concurrent.Future
import scala.concurrent.duration._
import com.roundeights.hasher.Implicits._

object AppMain2 extends App {
  val system = ActorSystem("cbft",ConfigFactory.load("node2.conf"))
  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher

  //加载数据库配置信息
  val dbconfig = system.settings.config.getConfig("database")
  MysqlConfig.initConfig(dbconfig.getString("driver"),dbconfig.getString("url"),dbconfig.getString("username"),dbconfig.getString("password"))

  //加载配置文件
  //println(system.settings.config.getConfig("cbft.node"))
  var nodemap = new HashMap[String,String]
  var schedulemap = new HashMap[String,Cancellable]
  val nodes = system.settings.config.getConfig("cbft.node").entrySet().asScala.foreach(entry => {
    nodemap.put(entry.getKey,entry.getValue.unwrapped().toString)
  })
  nodemap.toSeq.sortBy(_._1).toMap.foreach(entry => {
    NodesConfig.addNode(entry._1,entry._2)
  })
  NodeInfo.setHostName(system.settings.config.getString("cbft.hostname"))

  //本节点创建所有Actor
  val actorRefActor : ActorRef = system.actorOf(Props[ActorRefActor],"cbft_actorref")
  val requestActor : ActorRef = system.actorOf(Props[RequestActor].withMailbox("akka.actor.mymailbox"),"cbft_request")
  val requesthashsetActor : ActorRef = system.actorOf(Props[RequestHashSetActor],"cbft_requesthashset")
  val buildBlockActor : ActorRef = system.actorOf(Props[BuildBlockActor],"cbft_buildblock")
  val verifyBlockActor : ActorRef = system.actorOf(Props[VerifyBlockActor],"cbft_verifyblock")
  val blockVoteActor : ActorRef = system.actorOf(Props[BlockVoteActor],"cbft_blockvote")
  val blockVoteSetActor : ActorRef = system.actorOf(Props[BlockVoteSetActor],"cbft_blockvoteset")
  val blockChainActor : ActorRef = system.actorOf(Props[BlockChainActor],"cbft_blockchain")
  val storeBlockActor : ActorRef = system.actorOf(Props[StoreBlockActor],"cbft_storeblock")
  val cleanupActor : ActorRef = system.actorOf(Props[CleanupActor],"cbft_cleanup")
  val executeTransactionActor : ActorRef = system.actorOf(Props[ExecuteTransactionActor],"cbft_executetransaction")
  val updateStateActor : ActorRef = system.actorOf(Props(new UpdateStateActor("000001")),"cbft_updatestate")
  val onlineActor : ActorRef = system.actorOf(Props[NodeOnlineActor],"cbft_online")

  //检测节点是否启动
  //获取其他节点的ActorRef（用于cbft协议节点通信）
  nodemap foreach  (entry => {
    val nodename = entry._1
    val address = entry._2
    val cbft_online = system.actorSelection(s"akka.tcp://cbft@$address/user/cbft_online")
    //println(s"akka.tcp://cbft@$address/user/cbft_online")
    val schedule = system.scheduler.schedule(1 seconds,5 seconds,new Runnable{
      override def run(): Unit = {
        val msg = Identify(nodename)
        val identityf: Future[ActorIdentity] = (cbft_online ? msg).mapTo[ActorIdentity]
        identityf.onSuccess({
          case  ActorIdentity(`nodename`,Some(actorRef)) =>
            //接收到响应后
            onlineActor ! NodeOnline(nodename,true)
            actorRefActor ! NodeOnline(nodename,true)
          case ActorIdentity(`nodename`,None) =>
            println(nodename+" is still offline，wait 5 seconds and retry")
        })
      }
    })
    schedulemap.put(nodename,schedule)
  })
  val scheduleActor : ActorRef = system.actorOf(Props(new ScheduleActor(schedulemap)),"cbft_schedule")

  //println (system.settings.config.getValue("cbft.node1.hostname").render()+"-"+system.settings.config.getValue("cbft.node1.port").render())
}
