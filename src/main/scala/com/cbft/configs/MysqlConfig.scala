package com.cbft.configs

import java.sql.{Connection, DriverManager, SQLException}

object MysqlConfig {
  var driver = "com.mysql.jdbc.Driver"
  var url = "jdbc:mysql://localhost:3306/cbft"
  var username = "root"
  var password = "123456"

  def initConfig(db_driver : String,db_url : String,db_username : String,db_password : String) : Unit = {
    driver = db_driver
    url = db_url
    username = db_username
    password = db_password
  }

  def sqlconnection : Connection = {
    var conn: Connection = null
    try {
      // load mysql driver
      classOf[com.mysql.jdbc.Driver]
      conn = DriverManager.getConnection(url, username, password)
      return conn
    } catch {
      case e: SQLException => {
        e.printStackTrace
        return null
      }
    }
  }

}
