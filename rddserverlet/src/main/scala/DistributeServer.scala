import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.apache.spark.{SparkConf, SparkContext}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}

object DistributeServer {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("NodeCounter")
    if (!sparkConf.contains("spark.master")) {
      sparkConf.setMaster("local[*]")
    }

    sparkConf.set("spark.network.timeout", "300")

    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    sparkConf.set("spark.kryo.registrator", "org.locationtech.geomesa.spark.GeoMesaSparkKryoRegistrator")

    val sparkContext = new SparkContext(sparkConf)

    val port = if(args.isEmpty) 8013 else 8014
    val start = System.currentTimeMillis()
    val server = new Server(port);

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context)

    context.addServlet(new ServletHolder(new StatusSeverlet()), "/status");

    val host = server.getURI.getHost
    println(s"Server status at http://$host:$port/status")
    server.start();
    server.join();
  }
}

class StatusSeverlet extends HttpServlet {
  private val serialVersionUID: Long = 1L

  // 浏览器的url中只能是get请求，为了测试方便，这里get 和 post为同样的功能
  @throws[ServletException]
  @throws[IOException]
  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setStatus(HttpServletResponse.SC_OK)
    response.getOutputStream.print("Server is OK")
    response.setContentType("text")
  }
}
