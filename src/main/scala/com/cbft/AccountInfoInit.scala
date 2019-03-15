package com.cbft

import java.sql.SQLException

import com.cbft.configs.MysqlConfig

object AccountInfoInit extends App{

  val urls = List("jdbc:mysql://localhost:3306/cbft0","jdbc:mysql://localhost:3306/cbft1","jdbc:mysql://localhost:3306/cbft2","jdbc:mysql://localhost:3306/cbft3")

  urls.foreach(it => {
    MysqlConfig.url = it
    initAccount()
  })

  def initAccount(): Unit ={
    val baseid = "000001"
    val initamount = 10000
    var accounts : List[String] = List()
    for ( serialnum <- 0 to 9999){
      accounts = accounts :+ (baseid+"%010d".format(serialnum))
    }

    val conn = MysqlConfig.sqlconnection

    if(conn == null){
      System.exit(0)
    }
    conn.setAutoCommit(false)

    try {
      val sql = "INSERT INTO accountinfo(account_id,account_balance,account_pubkey) VALUES(?,?,?);"
      var pst = conn.prepareStatement(sql)
      var i = 0
      accounts.foreach(item => {
        pst.setString(1,item)
        pst.setDouble(2,initamount)
        pst.setString(3,"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        pst.addBatch()
        i+=1
        if(i%100==0){
          pst.executeBatch()
          conn.commit()
          pst.clearBatch()
        }
      })
      pst.executeBatch()
      conn.commit()
      pst.clearBatch()
      pst.close()
    } catch {
      case e: SQLException => e.printStackTrace
      case e: Exception => e.printStackTrace
    } finally {
      if (conn != null) {
        conn.close
      }
    }
  }
}
