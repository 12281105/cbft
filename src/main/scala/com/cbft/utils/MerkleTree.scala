package com.cbft.utils

import com.cbft.merkle.{Digest}

class MerkleTree(val hash: String,
                 val left: Option[MerkleTree] = Option.empty,
                 val right: Option[MerkleTree] = Option.empty) {

  def this(hash: String, left: MerkleTree, right: MerkleTree) =
    this(hash, Option(left), Option(right))

  override def toString =
    s"MerkleTree(hash=$hash)"
}

/** Companion object for [[MerkleTree]], acting as a factory. */
object MerkleTree {

  def apply(data: List[String], digest: Digest): MerkleTree = {
    var trees = data.map { block => new MerkleTree(block)}
    while (trees.length > 1) {
      trees = trees.grouped(2).map(make(digest, _)).toList
    }
    trees(0)
  }

  private[this] def make(digest: Digest, arr: Seq[MerkleTree]): MerkleTree = {
    if(arr.size==2){
      return new MerkleTree(merge(digest, arr), arr(0), arr(1))
    }
    else{
      return new MerkleTree(arr(0).hash,arr(0),null)
    }
  }

  private[this] def merge(digest: Digest, arr: Seq[MerkleTree]): String = {
    digest(arr(0).hash+arr(1).hash)
  }
}
