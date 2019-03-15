package com.cbft

import java.sql.{Connection, DriverManager, SQLException}
import java.util.Date

import com.cbft.configs.MysqlConfig
import com.cbft.TestMain.localhost
import com.cbft.messages.{Block, Request, Transaction}
import spray.json._
import com.cbft.messages.MessageJsonProtocol._
import com.cbft.utils.MysqlUtil
import com.roundeights.hasher.Implicits._

import scala.collection.mutable
import scala.collection.mutable.{HashMap, LinkedHashSet}
import scala.util.Random

class Student(val id: Int, val name: String, val age: Int) {
  override def toString = "id = " + id + ", name = " + name + ", age = " + age
}

object Test extends App {
  val random = new Random()
  println(random.nextInt(10000)+1)
  println(random.nextInt(10000)+1)
  println(random.nextInt(10000)+1)
  /*
  val test = new HashMap[String,Double]
  test.put("1",1)
  println(test.getOrElse("1",0))
  println(test.getOrElse("2",null))
  val a : String = null

  val hashSetMap = new HashMap[String,LinkedHashSet[String]]
  hashSetMap.put("1",LinkedHashSet("1","2","3","4"))
  hashSetMap.put("2",LinkedHashSet("3","4","5","6"))
  hashSetMap.put("3",LinkedHashSet("2","3","4","5"))
  hashSetMap.put("4",LinkedHashSet("1","2","5","6"))
  val res = hashSetMap.values.reduce(_ & _)
  println(res)

  if(0 >= (2/3.0*4)){
    println(true)
  }
  else{
    println(false)
  }

  val map1 = HashMap(21->"hello",1->"world",10->"nihao",3->"shijie")
  var sortmap = map1.toSeq.sortBy(_._1)
  println(sortmap)
  map1.put(5,"hehe")
  sortmap = map1.toSeq.sortBy(_._1)
  println(sortmap)

  val now = new Date()
  val nowtimestamp = now.getTime.toString.substring(0,10)
  val request : Request = Request("111".toString,Transaction("111","222","333",0,"444","555"),nowtimestamp,"666")
  val requestjson = request.toJson.toString
  println(requestjson)
  val request1 = requestjson.parseJson.convertTo[Request]
  println(request1.rtno)

  var conn = MysqlConfig.sqlconnection
  try {

    val stmt = conn.createStatement
    val sql = "select * from student"
    val rs = stmt.executeQuery(sql)
    while (rs.next) {
      println(new Student(rs.getInt(1), rs.getString(2), rs.getInt(3)))
    }
  } catch {
    case e: SQLException => e.printStackTrace
    case e: Exception => e.printStackTrace
  } finally {
    if (conn != null) {
      conn.close
    }
  }
  */

  /*
  val requests : LinkedHashSet[(String,String)] = new LinkedHashSet()
  for(i <- 1 to 100){
    val tx_id = (i+"").sha256.hex
    val from = i+""+i+""+i+""+i+""+i+""+i+""+i+""+i+""+i+""+i+""
    var to = i+""+i+""+i+""+i+""+i+""+i+""+i+""+i+""+i+""+i+""
    val amount = i*1.0
    val nowtimestamp = now.getTime.toString.substring(0,10)
    val tran = Transaction(tx_id,from,to,i,nowtimestamp,(from+to+amount+nowtimestamp).sha256.hex)
    val req = Request(i+"",tran,nowtimestamp,"localhost")
    requests.add((req.toJson.toString.sha256.hex,req.toJson.toString))
  }
  val block = Block(1,"111",nowtimestamp,"222","333",requests)
  MysqlUtil.writeBlock(block)
  */
  /*
  val newstates = new HashMap[String,Double]()
  conn = MysqlConfig.sqlconnection
  try {

    val stmt = conn.createStatement
    val sql = "select account_id from accountinfo"
    val rs = stmt.executeQuery(sql)
    while (rs.next) {
      //println(new Student(rs.getInt(1), rs.getString(2), rs.getInt(3)))
      newstates.put(rs.getString(1),1000)
    }
  } catch {
    case e: SQLException => e.printStackTrace
    case e: Exception => e.printStackTrace
  } finally {
    if (conn != null) {
      conn.close
    }
  }

  println(newstates)

  MysqlUtil.updateAccount(newstates)
  */

  /*
  val accountinfo = new HashMap[String,Double]()
  MysqlUtil.getBalanceAccounts("000001",accountinfo)
  println(accountinfo.size)
  */

}
