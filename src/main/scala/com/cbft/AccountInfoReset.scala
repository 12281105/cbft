package com.cbft

import java.sql.SQLException

import com.cbft.configs.MysqlConfig
import com.cbft.utils.MysqlUtil

import scala.collection.mutable.HashMap

object AccountInfoReset extends App{

  val urls = List("jdbc:mysql://localhost:3306/cbft0","jdbc:mysql://localhost:3306/cbft1","jdbc:mysql://localhost:3306/cbft2","jdbc:mysql://localhost:3306/cbft3")

  urls.foreach(it => {
    MysqlConfig.url = it
    resetAccount()
  })

  def resetAccount(): Unit ={
    val newstates = new HashMap[String,Double]()
    val conn = MysqlConfig.sqlconnection
    try {
      val stmt = conn.createStatement
      val sql = "select account_id from accountinfo"
      val rs = stmt.executeQuery(sql)
      while (rs.next) {
        newstates.put(rs.getString(1),10000)
      }
      rs.close()
      stmt.close()
    } catch {
      case e: SQLException => e.printStackTrace
      case e: Exception => e.printStackTrace
    } finally {
      if (conn != null) {
        conn.close()
      }
    }
    MysqlUtil.updateAccounts(newstates)
  }
}
