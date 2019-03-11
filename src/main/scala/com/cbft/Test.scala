package com.cbft

import scala.collection.mutable.{HashMap, LinkedHashSet}

object Test extends App {
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
}
