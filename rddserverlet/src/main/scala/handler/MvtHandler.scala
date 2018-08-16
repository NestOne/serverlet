package handler

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.supermap.bdt.mapping.render.MapRender
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

class MvtHandler(mapRender : MapRender) extends AbstractHandler{
  override def handle(target: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
    if(!target.endsWith(".mvt")){
      return;
    }

    println(target)

    val (level, col, row) = pathToURL(target)
    val bytes = mapRender.renderVector(level, col, row)
    if(bytes != null){
      httpServletResponse.getOutputStream.write(bytes)
      httpServletResponse.setStatus(HttpServletResponse.SC_OK)
    }else {
      httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
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
