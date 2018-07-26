import java.io.{File, FileOutputStream}

import com.supermap.bdt.geotools.jsonSer.JsonConverter
import com.supermap.bdt.mapping.render._
import com.supermap.bdt.mapping.util.tiling.CRS
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{DefaultHandler, HandlerList, ResourceHandler}
import org.json.JSONObject

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * 用于将DMap生成的矢量金字塔与原始数据发布成mvt服务
  * 多源的问题，需要测试Geomesa的Spark API，看看其Partition的情况是否符合多源的方案
  * 提供HDFS的缓存目录能力
  * 可以是合并的，也可是是单层的数据服务
  */
object mvtServer {
  //    这里用来测试功能的正确性，根据

  def main(args:Array[String]) ={
    val dataJson = args(0)
    var port = 8020
    if(args.length >1){
      port = args(1).toInt
    }

    //    生成html页面
    val resouceFolder = new File(dataJson).getParent
    var cacheName: String = "cacheName"
    val styleFile = s"$resouceFolder/styles/style.json"
    var centerX = 0.0
    var centerY = 0.0
    var zoom = 0.0
    val tileSize: Int = 512

    val lines = Source.fromFile(dataJson,"UTF-8").getLines().mkString("");
    val dataInfos = JsonConverter.parseJson(lines,classOf[Array[java.util.HashMap[String,String]]])

    val renders = new ArrayBuffer[LayerRender]()
    var crs = new CRS(4326)
    for( infoHash <- dataInfos){
      val info = new LayerRenderConfig("HBase")
      for( key <- infoHash.keySet().toArray){
        info.m_haspMap.put(key.toString,infoHash.get(key.toString))
      }

      if(info.typ == "HBase"){
        val reader = new HBaseLayerRender(info.m_haspMap.get("catalog"),info.m_haspMap.get("typeName"),crs,info.m_haspMap.get("zookeeper"))
        reader.m_idFieldName = info.m_haspMap.get("idField")

        reader.initialize()
        renders += reader
      }
    }
    TileCache.initTileLoader(renders)
    val server = new Server(port)
    val resource_handler = new ResourceHandler
    resource_handler.setDirectoriesListed(true)
    resource_handler.setResourceBase(resouceFolder)

    val handlers: HandlerList = new HandlerList
    handlers.setHandlers(Array(new EanbleCORSHandler(), new MvtHandler, resource_handler, new DefaultHandler))
    server.setHandler(handlers)

    val host = server.getURI.getHost

    if(new File(styleFile).exists()){
      val styleJson = Source.fromFile(styleFile,"UTF-8").mkString
      val jsonObj = new JSONObject(styleJson)
      cacheName = jsonObj.getString("name")
      val obj = jsonObj.get("metadata").asInstanceOf[JSONObject]
      zoom = obj.getDouble("zoom")
      val mpCenter = obj.getJSONArray("mapcenter")
      centerX = mpCenter.getDouble(0).doubleValue()
      centerY = mpCenter.getDouble(1).doubleValue()
      val indexBounds = crs.worldExtent
      val indexExtent = Array[Double](indexBounds.getLeft, indexBounds.getBottom, indexBounds.getRight, indexBounds.getTop)
      val newPt = HtmlGenerator.alterMBGLCenter(centerX,centerY,indexExtent,tileSize)
      val (editorX,editorY)=(newPt(0), newPt(1))
      var res0 = (indexBounds.getRight - indexBounds.getLeft)/tileSize
      val res0Y = (indexBounds.getTop - indexBounds.getBottom )/tileSize
      res0 = if ( res0Y > res0) res0Y else  res0
      val ress = new Array[Double](21)
      ress(0)=res0
      for( i <- 1 until 21){
        ress(i) = ress(i-1)/2
      }

      HtmlGenerator.generateOL4(resouceFolder,cacheName,crs.m_epsg,zoom.toInt,21,ress,centerX,centerY,indexExtent,tileSize)
      System.out.println(s"open with editor http://$host:$port/editor/index.html/$zoom/$editorY/$editorX")
      System.out.println(s"open by openlayers viewer http://$host:$port/index.html")
      System.out.println(s"open in mapbox-gl http://$host:$port/indexmbgl.html")
    }else{
      //      如果没有数据位置信息，需要重schema中获取需要的数据,无法获取到现在，需要后面看看怎办

    }



    server.start()
    server.join()


  }


}
