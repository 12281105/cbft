package com.cbft.actors

import java.util.Date

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, Cancellable, Identify, PoisonPill, Props, Status, Terminated}
import akka.event.Logging
import com.cbft.common.{NodeInfo, NodesActorRef}
import com.cbft.configs.NodesConfig
import com.cbft.messages._
import com.cbft.utils.{BroadcastUtil, MysqlUtil}
import com.roundeights.hasher.Implicits._

import scala.concurrent.duration._
import scala.collection.mutable.{HashMap, LinkedHashSet}
import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout

class NodeOnlineActor extends Actor{
  implicit val timeout = Timeout(5 seconds)
  val log = Logging(context.system, this)
  val online = new HashMap[String,Boolean]
  var actorRefReady : Int = 0
  val scheduleSelection : ActorSelection = context.actorSelection("/user/cbft_schedule")

  override def receive = {
    case NodeOnline(node: String,boolean: Boolean) => {
      val nodeb : String = NodesConfig.getNode(node)
      nodeb match {
        case x:String => {
          boolean match {
            case true => {
              log.info("node {} is online",node)
              online.put(node,boolean)
              scheduleSelection ! NodeOnline(node,true)
              //当上线的节点数等于配置文件中的节点数，向其他Actor发送启动信息(Broadcast)
              if(online.size == NodesConfig.NodeSize()){
                //online nodes ready
                log.info("NodeOnlineActor:{} online node ready",NodeInfo.getHostName())
                //所有节点都启动之后，ScheduleActor不再起作用，stop it
                scheduleSelection ! PoisonPill
                //如果本节点是主节点，向所有节点的RequestActor发送同步完成的消息
                if(actorRefReady == NodesConfig.NodeSize()){
                  NodesActorRef.getNodesActorRef("online").filterKeys(_ != NodeInfo.getHostName()).foreach(tuple => {context.watch(tuple._2)})
                  if(NodeInfo.isPrimary()) {
                    BroadcastUtil.BroadcastMessage(new SyncFinish)

                    //初始化完成后，创建创世块
                    val blocknum = MysqlUtil.getBlockNumber()
                    if(blocknum == 0) {
                      val init_hash = "0000000000000000000000000000000000000000000000000000000000000000"
                      val now = new Date()
                      val nowtimestamp = now.getTime.toString.substring(0,10)
                      val block = Block(0,init_hash,nowtimestamp,init_hash.sha256.hex,init_hash,new LinkedHashSet[(String,String)]())
                      BroadcastUtil.BroadcastMessage(GenesisBlock(NodeInfo.getHostName(),"0",block,""))
                    }
                  }
                }
              }
              else{

              }
            }
            case false => {  //其他节点主动向本节点发送NodeOnline（node，false）消息
              log.info("node {} is offline",node)
              online.put(node,boolean)
              //某个节点断开之后，可以开始新一轮的主节点选主(View Change) ??? unimplement

            }
          }
        }
        case null => {
          log.info("received unknown message: {}", NodeOnline)
          Status.Failure(new ClassNotFoundException)
        }
      }
    }
    case x : ActorRefReady => {
      actorRefReady += 1
      if(online.size == NodesConfig.NodeSize() && actorRefReady == NodesConfig.NodeSize()){
        //监控其他节点是否在线，NodeOnlineActor
        NodesActorRef.getNodesActorRef("online").filterKeys(_ != NodeInfo.getHostName()).foreach(tuple => {context.watch(tuple._2)})
        log.info("NodeOnlineActor:{} actorRef ready",NodeInfo.getHostName())
        if(NodeInfo.isPrimary()){
          BroadcastUtil.BroadcastMessage(new SyncFinish)

          //初始化完成后，创建创世块
          val blocknum = MysqlUtil.getBlockNumber()
          if(blocknum == 0) {
            val init_hash = "0000000000000000000000000000000000000000000000000000000000000000"
            val now = new Date()
            val nowtimestamp = now.getTime.toString.substring(0,10)
            val block = Block(0,init_hash,nowtimestamp,init_hash.sha256.hex,init_hash,new LinkedHashSet[(String,String)]())
            BroadcastUtil.BroadcastMessage(GenesisBlock(NodeInfo.getHostName(),"0",block,""))
          }
        }
      }
    }
    case Terminated(actorRef) =>{
      val nodename = NodesActorRef.getNodesActorRef("online").filter(tuple => tuple._2==actorRef).keys.head
      println(nodename)
      //如果是主节点，向其他节点RequestActor发送NodeOnline（nodename，"false"）
      if(NodeInfo.isPrimary()) {
        BroadcastUtil.BroadcastMessage(new NodeOnline(nodename, false))
      }

      //删除该节点的在线信息
      online.remove(nodename)
      actorRefReady = 0
      val actorRefActor = NodesActorRef.getNodeActorRef("actorref", NodeInfo.getHostName())
      actorRefActor ! NodeOnline(nodename, false)

      //开启schedule检测断开连接的节点是否重新连接
      import context.dispatcher
      val cbft_online = context.system.actorSelection(s"akka.tcp://cbft@${NodesConfig.getNode(nodename)}/user/cbft_online")
      val schedule = context.system.scheduler.schedule(3 seconds, 5 seconds, new Runnable {
        override def run(): Unit = {

        val msg = Identify(nodename)
        val identityf: Future[ActorIdentity] = (cbft_online ? msg).mapTo[ActorIdentity]
        identityf.onSuccess({
          case  ActorIdentity(`nodename`,Some(actorRef)) =>
            //接收到响应后
            actorRefActor ! NodeOnline(nodename,true)
            self ! NodeOnline(nodename,true)
          case ActorIdentity(`nodename`,None) =>
            println(nodename+" is still offline，wait 5 seconds and retry")
        })

        }
      })

      val cbft_schedule = context.actorSelection("/user/cbft_schedule")

      val msg = Identify(nodename)
      val identityf: Future[ActorIdentity] = (cbft_schedule ? msg).mapTo[ActorIdentity]
      identityf.onSuccess({
        case  ActorIdentity(`nodename`,Some(actorRef)) =>
          //接收到响应后
          actorRef ! ScheduleCancel(nodename,schedule)
        case ActorIdentity(`nodename`,None) =>
          var schedulemap = new HashMap[String, Cancellable]
          schedulemap.put(nodename, schedule)
          val scheduleActor: ActorRef = context.system.actorOf(Props(new ScheduleActor(schedulemap)), "cbft_schedule")
      })

    }
    case o => {
      log.info("received unknown message: {}", o)
      Status.Failure(new ClassNotFoundException)
    }
  }
}
