import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.supermap.bdt.mapping.render.HBaseLayerRender
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

class HBaseHandler(hBaseFeatureRender : HBaseLayerRender) extends AbstractHandler{
  override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
    if(!s.endsWith(".mvt")){
      return;
    }

    val start = System.currentTimeMillis()
    val (level, col, row) = pathToURL(s)

    val bytes = hBaseFeatureRender.makeVectorTile(level, col, row)

    if(bytes != null){
      httpServletResponse.getOutputStream.write(bytes)
      httpServletResponse.setStatus(HttpServletResponse.SC_OK)
    }else {
      httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }

    request.setHandled(true)

    println(s"buildVectorTile ${level}/$col/$row.mvt Cost: " + (System.currentTimeMillis() - start) + "ms")
  }

  def pathToURL(path : String) : (Int, Int, Int)={
    val subs = path.split("/")
    val levelStr = subs(subs.length - 3)
    val columnStr = subs(subs.length - 2)

    var rowStr = subs(subs.length - 1)
    rowStr = rowStr.substring(0, rowStr.length - 4)

    (levelStr.toInt, columnStr.toInt, rowStr.toInt)
  }


}
