package com.cbft

import java.sql.SQLException

import com.cbft.configs.MysqlConfig

import scala.collection.mutable.HashMap

object DatabaseInfoClear extends App{
  val urls = List("jdbc:mysql://localhost:3306/cbft0","jdbc:mysql://localhost:3306/cbft1","jdbc:mysql://localhost:3306/cbft2","jdbc:mysql://localhost:3306/cbft3")

  urls.foreach(it => {
    MysqlConfig.url = it
    clearDatabase()
  })

  def clearDatabase(): Unit ={
    val conn = MysqlConfig.sqlconnection
    try {
      val stmt = conn.createStatement
      var sql = "truncate table accountinfo"
      var rs = stmt.execute(sql)
      sql = "truncate table blockinfo"
      rs = stmt.execute(sql)
      sql = "truncate table transactioninfo"
      rs = stmt.execute(sql)
      stmt.close()
    } catch {
      case e: SQLException => e.printStackTrace
      case e: Exception => e.printStackTrace
    } finally {
      if (conn != null) {
        conn.close()
      }
    }
  }

}
