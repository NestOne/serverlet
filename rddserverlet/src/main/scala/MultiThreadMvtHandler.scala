import java.net.URLDecoder

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.StringUtils
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

class MultiThreadMvtHandler extends AbstractHandler {
  override def handle(target: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
    if(!target.endsWith(".mvt")){
      return;
    }

    var queryString = request.getQueryString()
    var tileKey:String = ""
    if(!StringUtils.isBlank(queryString)){
      tileKey = URLDecoder.decode(queryString,"UTF-8")
    }
    val (tileSource, zoom, col, row) = TileCache.pathToURL(target)
    tileKey = s"$zoom/$col/$row?$tileKey"
    val tile = TileDataCache.getTile(zoom,col,row,tileKey)

    if(tile != null && tile.length > 0){
      httpServletResponse.getOutputStream.write(tile)
      httpServletResponse.setStatus(HttpServletResponse.SC_OK)
    }else {
      httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
    request.setHandled(true)
  }
}
