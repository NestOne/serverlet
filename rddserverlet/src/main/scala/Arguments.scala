import scala.collection.mutable

object Arguments {
  def apply(args: Array[String]): mutable.HashMap[String, String] = {
    val dic = new mutable.HashMap[String, String]
    args.foreach( arg => {
      if(arg.startsWith("-") && arg.contains("=")){
        val parts = arg.split("=")
        var key = parts(0).trim
        val value = parts(1).trim

        key = key.replace("-", "")

        dic += (key -> value)
      }
    })

    dic
  }
}
