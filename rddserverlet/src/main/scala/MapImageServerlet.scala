import java.awt.{Color, Dimension}
import java.io._
import java.net.URLDecoder
import java.util.ServiceLoader
import javax.imageio.ImageIO
import javax.servlet.ServletException
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.alibaba.fastjson.{JSON, JSONObject}
import com.supermap.bdt.mapping.DMap
import com.supermap.data.{Point2D, Rectangle2D, Workspace, WorkspaceConnectionInfo}
import org.apache.commons.io.FileUtils
import org.locationtech.geomesa.hbase.data.HBaseDataStoreParams.HBaseCatalogParam
import org.locationtech.geomesa.spark.SpatialRDDProvider

import scala.collection.JavaConversions._

/**
  * Created by Administrator on 2017/8/30.
  */
abstract class  MapImageServerlet(m_dmap: DMap) extends HttpServlet{
  private val serialVersionUID: Long = 1L
  
   def getImage(level : Int, col : Int, row : Int):Array[Byte]

  @throws[ServletException]
  @throws[IOException]
  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    //response.setStatus(HttpServletResponse.SC_OK)
    val args = URLDecoder.decode(request.getQueryString(),"UTF-8").split("&")
    val hashValues = new scala.collection.mutable.HashMap[String,String]()
    for( i <- 0 until args.length){
      val keyAndValue = args(i).split("=");
      hashValues.put(keyAndValue(0),keyAndValue(1))
    }

    val level:Int = hashValues.get("l").getOrElse("5").toInt
    val col:Int = hashValues.get("x").getOrElse("1").toInt
    val row:Int = hashValues.get("y").getOrElse("1").toInt

    // TODO 支持处理ViewBounds的请求情况
    val start = System.currentTimeMillis()
    val bits:Array[Byte] = this.getImage(level,col,row)

    println("=======================================================")
   // println("get image info: scale " + scale + "| x " + x + "| y " + y + "| col " + col + "| row" + row)
    println("getImage cost " + (System.currentTimeMillis() -start))
    if(bits != null){
      response.getOutputStream.write(bits)
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

// 保存文件
//    val fileName =  level + "_" + col + "_" + row + ".jpg";
//    val filePath = "F:\\nanning\\nanning_3857\\DLTB_1_2w@nanning\\" + fileName
//    val outputfile = new File(filePath)

//    val result = new ByteArrayOutputStream()
//    result.write(bits, 0, bits.length)

//    import java.io.FileOutputStream
//    import java.io.OutputStream
//    val output = new FileOutputStream(outputfile)
//    try
//      output.write(bits)
//    finally output.close()
  }
}

class RDDHdfsServerlet(m_dmap: DMap) extends MapImageServerlet(m_dmap){
  override def getImage(level : Int, col : Int, row : Int):Array[Byte] ={
    m_dmap.renderBitmap(level, col, row)
  }
}

class MVTServerlet(m_dmap: DMap) extends MapImageServerlet(m_dmap){
  override def getImage(level : Int, col : Int, row : Int):Array[Byte] ={
    //m_dmap.renderVector(level, col, row)
    val basePath = s"F:\\nanning\\nanning_3857\\DLTB_2w_Double@nanning\\tiles\\$level\\$col\\$row.mvt"
    val file = new File(basePath)
    if(file.exists()){
      FileUtils.readFileToByteArray(file)
    } else null
  }
}


class StatusSeverlet extends HttpServlet {
  private val serialVersionUID: Long = 1L


  // 浏览器的url中只能是get请求，为了测试方便，这里get 和 post为同样的功能
  @throws[ServletException]
  @throws[IOException]
  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setStatus(HttpServletResponse.SC_OK)
    response.setContentType("text")
  }
}