package handler

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.supermap.bdt.mapping.render.MapRender
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

class MultiSourceMvtHandler(mapRender : MapRender) extends AbstractHandler{
  override def handle(target: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
    if(!target.endsWith(".mvt")){
      return;
    }

    println(target)

    val (jute, level, col, row) = pathToURL(target)
    val bytes = mapRender.renderVector(level, col, row, filter = jute.toString)
    if(bytes != null){
      httpServletResponse.getOutputStream.write(bytes)
      httpServletResponse.setStatus(HttpServletResponse.SC_OK)
    }else {
      httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
  }

  def pathToURL(path : String) : (Int, Int, Int, Int)={
    val subs = path.split("/")
    val caption = subs(subs.length - 4)
    val jute =
      if(caption == "3") 0
      else if(caption == "2") 1
      else 2
    val levelStr = subs(subs.length - 3)
    val columnStr = subs(subs.length - 2)

    var rowStr = subs(subs.length - 1)
    rowStr = rowStr.substring(0, rowStr.length - 4)

    (jute, levelStr.toInt, columnStr.toInt, rowStr.toInt)
  }
}
