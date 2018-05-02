import java.net.URI

import com.supermap.bdt.mapping.{DLayerPyramidData, DMap}
import com.supermap.mapping.LayerSettingVector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}

object Startup_Pyramid {

  def main(args: Array[String]): Unit = {
    // args(0) 原始层索引文件位置
    // args(1) 金字塔文件存储位置
    // args(2）图层模板路径或内容字符串

    val start = System.currentTimeMillis()

    val server = new Server(8010);
    val context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context)
    if (args.length >= 2) {
      val dMap = new DMap()
      dMap.initialize()

      val dStart = System.currentTimeMillis()
      println("start buildhdfsIndex")

      val originalUri = args(0)
      val pyramidUri = args(1)

      val layerdata = DLayerPyramidData.loadPyramidLayerData(dMap.m_sparkContext, originalUri, pyramidUri)

      if (args.length == 3) {
        dMap.addDLayerViaTemplate(layerdata, 0, args(2))
      }else{
        val layerSetting = new LayerSettingVector()
        dMap.addDLayer(layerdata, 0, "Test", layerSetting)
      }

      println("end buildhdfsIndex.(ms)", System.currentTimeMillis() - dStart)
      context.addServlet(new ServletHolder(new RDDHdfsServerlet(dMap)), "/image.png");
    }

    println("start cost(ms):", System.currentTimeMillis() - start)
    server.start();
    server.join();

  }
}
