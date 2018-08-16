
import java.io.File

import com.supermap.bdt.mapping.render.{HBaseLayerRender, LayerRenderConfig, MapRender}
import com.supermap.bdt.mapping.util.tiling.CRS
import handler.MultiSourceMvtHandler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{DefaultHandler, HandlerList, ResourceHandler}
import util.Arguments

object StartupMVT {
  def main(args: Array[String]): Unit = {
    val dt = "0x0D0D0D"

    val fC = Integer.valueOf(dt.replace("0x", ""), 16);

    if (args.length > 1) {
      val start = System.currentTimeMillis()

      val arguments = Arguments(args)

      val workspacePath = arguments.get("workspacePath").getOrElse({
        println("need arg 'workspacePath' like -workspace=nanning.smwu")
        return })

      val mapName = arguments.get("mapName").getOrElse({
        println("need arg 'mapName' like -mapName=DLTB_2w_Double")
        return })

      val htmlPath = arguments.get("htmlPath").getOrElse({
        println("need arg 'htmlPath' like -htmlPath=/home/index.html")
        return })


      // 可选参数
      val port : Int = arguments.get("port").getOrElse("8013").toInt
      val zookeeper = arguments.get("zookeeper").getOrElse(null)

      println("start initialize mapRender")

      val mapRender = new MapRender()
      mapRender.initialize(workspacePath, mapName)

      println("initialize mapRender cost " + (System.currentTimeMillis() - start) + "ms")

      val server = new Server(port);

      val htmlFile = new File(htmlPath)
      val htmlFolder = htmlFile.getParent
      val resource_handler = new ResourceHandler
      resource_handler.setDirectoriesListed(true)
      resource_handler.setResourceBase(htmlFolder)

      val handlers: HandlerList = new HandlerList
      handlers.setHandlers(Array(new EanbleCORSHandler(), new MultiSourceMvtHandler(mapRender), resource_handler, new DefaultHandler))
      server.setHandler(handlers)

      val host = server.getURI
      System.out.println("open by openlayers viewer http://" + host.getHost + ":" + port + "/" + htmlFile.getName)

      println("start cost(ms):", System.currentTimeMillis() - start)

      server.start()
      server.join()
    }
    else{
      println("need args like -workspacePath=nanning -mapName=nanning_2w_1")
    }
  }
}