package util


import scala.collection.mutable

class Arguments(){
  private val m_hash : mutable.HashMap[String, String] = new mutable.HashMap[String, String]

  def add(key : String, value : String): Unit ={
    m_hash += (key -> value)
  }

  def get(key : String) : Option[String] ={
      m_hash.get(key)
  }
}

object Arguments {
  def apply(args: Array[String]): Arguments = {
    val arguments = new Arguments
    args.foreach( arg => {
      if(arg.contains("=")){
        val parts = arg.split("=")
        val key = parts(0).replaceFirst("--", "").replaceFirst("-", "").trim
        val value = if(parts.size > 1) parts(1).trim
        else ""

        arguments.add(key, value)
      }
    })

    arguments
  }
}
