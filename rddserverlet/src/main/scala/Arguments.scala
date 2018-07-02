import scala.collection.mutable

object Arguments {
  def apply(args: Array[String]): mutable.HashMap[String, String] = {
    val dic = new mutable.HashMap[String, String]
    args.foreach( arg => {
      if(arg.startsWith("-") && arg.contains("=")){
        val parts = arg.split("=")
        var key = parts(0)
        val value = parts(1)

        key = key.replace("-", "")

        dic += (key -> value)
      }
    })

    dic
  }
}
