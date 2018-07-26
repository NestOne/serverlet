import java.net.URLDecoder

import com.google.common.cache.{CacheLoader, LoadingCache}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.supermap.bdt.mapping.render.{HBaseLayerRender, LayerRender, MVTRenderEngine, MapRender}
import com.supermap.bdt.mapping.util.tiling.CRS
import org.apache.commons.lang3.StringUtils
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

import scala.collection.mutable.ArrayBuffer

/**
  * 通过tiles请求时为所有的图层合并到一个mvt tile中，如果是用dataset@catalog的方式请求，则为分层显示，方便配图使用
  * 同时使用hdfs API进行文件缓存，以便获得更好的并发性能
  */
class MvtHandler extends AbstractHandler {

  override def handle(target: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
    if(!target.endsWith(".mvt")){
      return;
    }
    var queryString = ""
    if(request.getQueryString() != null){
      queryString = URLDecoder.decode(request.getQueryString(),"UTF-8")
      val args = queryString.split("&")
      val hashValues = new scala.collection.mutable.HashMap[String,String]()
      for( i <- 0 to args.length){
        val keyAndValue = args(i).split("=");
        hashValues.put(keyAndValue(0),keyAndValue(1))
      }
      val keyField = if(hashValues.contains("filed")){
        hashValues.get("field").toString
      }else{
        ""
      }
    }



    val tileKey = if(StringUtils.isBlank(queryString)){
      "all"
    }else{
      queryString
    }
    var tiles = TileCache.getTile(target)
    val tile:Array[Byte] = if(tiles != null){
      tiles.get(tileKey)
    }else{
      null
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
