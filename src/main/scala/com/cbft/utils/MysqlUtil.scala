package com.cbft.utils

import java.sql.{Connection, SQLException}

import com.cbft.configs.MysqlConfig
import com.cbft.messages.{Block, Request}
import com.roundeights.hasher.Implicits._
import spray.json._
import com.cbft.messages.MessageJsonProtocol._

import scala.collection.mutable.HashMap

object MysqlUtil {
  //将区块信息写入数据库
  def writeBlock(block : Block): Boolean ={
    val conn = MysqlConfig.sqlconnection
    if(conn == null){
      return false
    }
    conn.setAutoCommit(false)
    try{
      //将区块头信息写入数据库
      var sql = "INSERT INTO blockinfo(block_height,pre_hash,cur_hash,gen_time,merkle_root,trans_num) VALUES(?,?,?,?,?,?)"
      var pst = conn.prepareStatement(sql)
      pst.setInt(1,block.height)
      pst.setString(2,block.pre_hash)
      pst.setString(3,block.cur_hash)
      pst.setString(4,block.timestamp)
      pst.setString(5,block.merkle_root)
      pst.setInt(6,block.requests.size)
      pst.execute()
      pst.close()

      //将区块中的交易信息写入数据库
      sql = "INSERT INTO transactioninfo(tx_id,tx_from,tx_to,tx_amount,tx_time,tx_sign,tx_index,block_height) VALUES(?,?,?,?,?,?,?,?)"
      pst = conn.prepareStatement(sql)
      var i = 0
      block.requests.foreach(tuple => {
        val requestjson = tuple._2.parseJson
        val request = requestjson.convertTo[Request]
        val transaction = request.tx
        pst.setString(1,transaction.txid)
        pst.setString(2,transaction.from)
        pst.setString(3,transaction.to)
        pst.setDouble(4,transaction.amount)
        pst.setString(5,transaction.gentime)
        pst.setString(6,transaction.sign)
        pst.setInt(7,i)
        pst.setInt(8,block.height)
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

      return true
    }catch {
      case e: SQLException => {
        e.printStackTrace
        return false
      }
    }finally {
      if (conn != null) {
        conn.close
      }
    }
  }

  def updateAccounts(newstates : HashMap[String,Double]): Boolean ={
    val conn = MysqlConfig.sqlconnection
    if(conn == null){
      return false
    }
    conn.setAutoCommit(false)
    try{
      //将区块头信息写入数据库
      var sql = "UPDATE accountinfo SET account_balance = ? WHERE account_id = ?"
      var pst = conn.prepareStatement(sql)
      var i = 0
      newstates.foreach(tuple => {
        pst.setDouble(1,tuple._2)
        pst.setString(2,tuple._1)
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
      return true
    }catch {
      case e: SQLException => {
        e.printStackTrace
        return false
      }
    }finally {
      if (conn != null) {
        conn.close
      }
    }
  }

  def getBalanceAccounts(baseid : String,accountinfo : HashMap[String,Double]): Unit ={
    val conn = MysqlConfig.sqlconnection
    if(conn == null){
      return false
    }
    try{
      //将区块头信息写入数据库
      var sql = "SELECT account_id,account_balance FROM accountinfo WHERE account_id LIKE ?"
      var pst = conn.prepareStatement(sql)
      pst.setString(1,baseid+"%")
      val rs = pst.executeQuery()
      while (rs.next) {
        accountinfo.put(rs.getString(1),rs.getDouble(2))
      }
      pst.close()
    }catch {
      case e: SQLException => {
        e.printStackTrace
      }
    }finally {
      if (conn != null) {
        conn.close
      }
    }
  }

}
