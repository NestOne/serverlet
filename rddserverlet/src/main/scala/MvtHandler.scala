import java.net.URLDecoder

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.StringUtils
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import scala.collection.JavaConversions._

/**
  * 通过tiles请求时为所有的图层合并到一个mvt tile中，如果是用dataset@catalog的方式请求，则为分层显示，方便配图使用
  * 同时使用hdfs API进行文件缓存，以便获得更好的并发性能
  */
class MvtHandler extends AbstractHandler {

  override def handle(target: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
    if(!target.endsWith(".mvt")){
      return;
    }
    var queryString = request.getQueryString()
    var tileKey:String = "all"
    if(!StringUtils.isBlank(queryString)){
      queryString = URLDecoder.decode(queryString,"UTF-8")
      val args = queryString.split("&")
      val hashValues = new java.util.HashMap[String,String]()
      for( i <- 0 until args.length){
        val str = args(i)
        val index = str.indexOf("=")
        hashValues.put(str.substring(0,index),str.substring(index+1))
      }
      if(hashValues.contains("sourceLayer")){
        tileKey = hashValues.get("sourceLayer")
      }
    }
    var tiles = TileCache.getTile(target)
    var tile:Array[Byte] = null
    if(tiles != null){
      tile = tiles.get(tileKey)
    }

    if(tile != null){
      httpServletResponse.getOutputStream.write(tile)
      httpServletResponse.setStatus(HttpServletResponse.SC_OK)
    }else {
      httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
    request.setHandled(true)
  }


}
