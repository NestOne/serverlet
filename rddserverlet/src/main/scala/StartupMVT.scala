
import java.io.File

import com.supermap.bdt.mapping.render.{HBaseLayerRender, MapRender}
import com.supermap.bdt.mapping.util.tiling.CRS
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{DefaultHandler, HandlerList, ResourceHandler}

object StartupMVT {
  def main(args: Array[String]): Unit = {
    val dt = "0x0D0D0D"

    val fC = Integer.valueOf(dt.replace("0x", ""), 16);

    if (args.length > 1) {
      val start = System.currentTimeMillis()

      val argDic = Arguments(args)

      val workspacePath =
        if(!argDic.contains("workspacePath")){
          println("need arg 'workspacePath' like -workspacePath=nanning.smw")
          return
        }else{
          argDic("workspacePath")
        }

      val mapName =
        if(!argDic.contains("mapName")){
          println("need arg 'mapName' like -mapName=DLTB_2w_Double")
          return
        }else{
          argDic("mapName")
        }

      val htmlPath =
        if(!argDic.contains("htmlPath")){
          println("need arg 'htmlPath' like -htmlPath=/home/index.html")
          return
        }else{
          argDic("htmlPath")
        }

      // 可选参数
      val port : Int = argDic.getOrElse("port", "8013").toInt
      val zookeeper = argDic.getOrElse("zookeeper", null)

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
      handlers.setHandlers(Array(new MultiSourceMvtHandler(mapRender), resource_handler, new DefaultHandler))
      server.setHandler(handlers)

      val host = server.getURI
      System.out.println("open by openlayers viewer http://" + host.getHost + ":" + 8013 + "/" + htmlFile.getName)

      println("start cost(ms):", System.currentTimeMillis() - start)

      server.start()
      server.join()
    }
    else{
      println("need args like -workspacePath=nanning -mapName=nanning_2w_1")
    }
  }
}