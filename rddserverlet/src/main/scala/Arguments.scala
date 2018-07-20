
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
      if(arg.startsWith("-") && arg.contains("=")){
        val parts = arg.split("=")
        var key = parts(0).trim
        val value = parts(1).trim

        key = key.replace("-", "")

        arguments.add(key, value)
      }
    })

    arguments
  }
}
