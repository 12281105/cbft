package com.cbft

import akka.actor.{ActorIdentity, ActorRef, ActorSystem, Cancellable, Identify, Props}
import akka.util.Timeout
import com.cbft.actors._
import com.cbft.common.NodeInfo
import com.cbft.configs.{MysqlConfig, NodesConfig}

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import akka.pattern.ask
import com.cbft.messages._
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.HashMap
import scala.concurrent.Future
import com.roundeights.hasher.Implicits._

object AppMain0 extends App {
  //创建ActorSystem
  val system = ActorSystem("cbft",ConfigFactory.load("node0.conf"))
  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher

  //加载数据库配置信息
  val dbconfig = system.settings.config.getConfig("database")
  MysqlConfig.initConfig(dbconfig.getString("driver"),dbconfig.getString("url"),dbconfig.getString("username"),dbconfig.getString("password"))

  //加载配置文件
  var nodemap = new HashMap[String,String]
  var schedulemap = new HashMap[String,Cancellable]
  //读取配置文件中的节点信息
  val nodes = system.settings.config.getConfig("cbft.node").entrySet().asScala.foreach(entry => {
    nodemap.put(entry.getKey,entry.getValue.unwrapped().toString)
  })
  //NodesConfig 保存所有节点的IP-PORT信息
  nodemap.toSeq.sortBy(_._1).toMap.foreach(entry => {
    NodesConfig.addNode(entry._1,entry._2)
  })
  //NodeInfo 保存本节点的配置信息
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
  val executeTransactionActor : ActorRef = system.actorOf(Props[ExecuteTransactionActor],"cbft_executetransaction")
  val updateStateActor : ActorRef = system.actorOf(Props(new UpdateStateActor("000001")),"cbft_updatestate")
  val onlineActor : ActorRef = system.actorOf(Props[NodeOnlineActor],"cbft_online")

  //检测节点启动
  //获取其他节点的ActorRef（用于cbft协议节点通信）
  nodemap foreach  (entry => {
      val nodename = entry._1
      val address = entry._2
      val cbft_online = system.actorSelection(s"akka.tcp://cbft@$address/user/cbft_online")
      //println(s"akka.tcp://cbft@$address/user/cbft_online")
      val schedule = system.scheduler.schedule(1 seconds,5 seconds,new Runnable{
        override def run(): Unit = {
          val msg = Identify(nodename)
          //检测节点是否已经在线
          val identityf: Future[ActorIdentity] = (cbft_online ? msg).mapTo[ActorIdentity]
          identityf.onSuccess({
            case  ActorIdentity(`nodename`,Some(actorRef)) =>
              //接收到响应后，表示节点的ActorSystem已经启动在线
              actorRefActor ! NodeOnline(nodename,true)
              onlineActor ! NodeOnline(nodename,true)
            case ActorIdentity(`nodename`,None) =>
              println(nodename+" is still offline，wait 5 seconds and retry")
          })
        }
      })
      schedulemap.put(nodename,schedule)
  })
  val scheduleActor : ActorRef = system.actorOf(Props(new ScheduleActor(schedulemap)),"cbft_schedule")

  //初始化完成后，创建创世块
  val block = Block(0,"0","0","0","0".sha256.hex,null)
  system.actorSelection("/user/cbft_blockchain") ! GenesisBlock(NodeInfo.getHostName(),"0",block,"")

  //println (system.settings.config.getValue("cbft.node1.hostname").render()+"-"+system.settings.config.getValue("cbft.node1.port").render())
}
