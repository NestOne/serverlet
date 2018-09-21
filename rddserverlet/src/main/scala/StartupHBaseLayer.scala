
import java.io.File

import com.supermap.bdt.mapping.render.HBaseLayerRender
import com.supermap.bdt.mapping.util.tiling.CRS
import handler.HBaseHandler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{DefaultHandler, HandlerList, ResourceHandler}
import util.Arguments

object StartupHBaseLayer {
  def main(args: Array[String]): Unit = {
    val dt = "0x0D0D0D"

    val fC = Integer.valueOf(dt.replace("0x", ""), 16);

    if (args.length > 1) {
      val start = System.currentTimeMillis()
      // 测试shapeless依赖问题
      val point = geotrellis.vector.Point(1, 32)

      val arguments = Arguments(args)

      val tableName = arguments.get("tableName").getOrElse({
        println("need arg 'tableName' like -tableName=nanning")
        return })

      val typeName = arguments.get("typeName").getOrElse({
        println("need arg 'typeName' like -typeName=DLTB_2w_Double")
        return })

      val epsg = arguments.get("epsg").getOrElse({
        println("need arg 'epsg' like -epsg=3857")
        return })

      val htmlPath = arguments.get("htmlPath").getOrElse({
        println("need arg 'htmlPath' like -htmlPath=/home/index.html")
        return })

      // 可选参数
      val port : Int = arguments.get("port").getOrElse("8013").toInt
      val zookeeper = arguments.get("zookeeper").getOrElse(null)

      println("start open hbase datastore")

      val hBaseRender = new HBaseLayerRender(tableName, typeName, zookeeper = zookeeper)
      hBaseRender.initialize()

      println("open hbase datastore cost " + (System.currentTimeMillis() - start) + "ms")

      val server = new Server(port);

      val htmlFile = new File(htmlPath)
      val htmlFolder = htmlFile.getParent
      val resource_handler = new ResourceHandler
      resource_handler.setDirectoriesListed(true)
      resource_handler.setResourceBase(htmlFolder)

      val handlers: HandlerList = new HandlerList
      handlers.setHandlers(Array(new HBaseHandler(hBaseRender), resource_handler, new DefaultHandler))
      server.setHandler(handlers)

      val host = server.getURI
      System.out.println("open by openlayers viewer http://" + host.getHost + ":" + 8013 + "/" + htmlFile.getName)

      println("start cost(ms):", System.currentTimeMillis() - start)

      server.start()
      server.join()
    }
    else{
      println("need args like -tableName=nanning -typeName=nanning_2w_1")
    }
  }
}