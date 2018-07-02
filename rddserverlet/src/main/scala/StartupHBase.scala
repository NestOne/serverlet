
import java.io.File

import com.supermap.bdt.mapping.{DMap, HBaseFeatureRender}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{DefaultHandler, HandlerList, ResourceHandler}

object StartupHBase {
  def main(args: Array[String]): Unit = {
    val dt = "0x0D0D0D"

    val fC = Integer.valueOf(dt.replace("0x", ""), 16);

    if (args.length > 1) {
      val start = System.currentTimeMillis()
      // 测试shapeless依赖问题
      val point = geotrellis.vector.Point(1, 32)

      val argDic = Arguments(args)

      val tableName =
        if(!argDic.contains("tableName")){
          println("need arg 'tableName' like -tableName=nanning")
          return
        }else{
          argDic("tableName")
        }

      val typeName =
        if(!argDic.contains("typeName")){
          println("need arg 'typeName' like -typeName=DLTB_2w_Double")
          return
        }else{
          argDic("typeName")
        }


      val epsg =
        if(!argDic.contains("epsg")){
          println("need arg 'epsg' like -epsg=3857")
          return
        }else{
          argDic("typeName")
        }

      val htmlPath =
        if(!argDic.contains("htmlPath")){
          println("need arg 'htmlPath' like -htmlPath=/home/index.html")
          return
        }else{
          argDic("htmlPath")
        }

      // 可选参数
      val port : Int =
        if(argDic.contains("port")){
          argDic("port").toInt
        }else{
          8013
        }

      val zookeeper =
        if(argDic.contains("zookeeper")){
          argDic("zookeeper")
        }else{
          null
        }

      //"DLTB_1_2w"//args(1)
     // val htmlPath= "F:\\nanning\\nanning_3857\\DLTB_2w_Double@nanning\\index.html"//args(2)

      println("start open hbase datastore")

      val hBaseRender = new HBaseFeatureRender(tableName, typeName, zookeeper = zookeeper, epsg = epsg.toInt)

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