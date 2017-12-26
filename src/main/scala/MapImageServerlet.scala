import java.awt.{Color, Dimension}
import java.io._
import java.net.URLDecoder
import javax.servlet.ServletException
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}


import com.alibaba.fastjson.{JSON, JSONObject}
import com.supermap.bdt.mapping.DMap
import com.supermap.data.{Point2D, Rectangle2D, Workspace, WorkspaceConnectionInfo}

/**
  * Created by Administrator on 2017/8/30.
  */
abstract class  MapImageServerlet(m_dmap: DMap) extends HttpServlet{
  private val serialVersionUID: Long = 1L
  
   def getImage(scale:Double, center:Point2D, width:Int, height:Int,tile:Int, filter:Boolean, viewBounds:Rectangle2D):Array[Byte]


  @throws[ServletException]
  @throws[IOException]
  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(HttpServletResponse.SC_OK)
    val args = URLDecoder.decode(request.getQueryString(),"UTF-8").split("&")
    val hashValues = new scala.collection.mutable.HashMap[String,String]()
    for( i <- 0 until args.length){
      val keyAndValue = args(i).split("=");
      hashValues.put(keyAndValue(0),keyAndValue(1))
    }

    var scale:Double = hashValues.get("scale").getOrElse("4.9014683554480507429591568955777e-7").toDouble
    var width:Int = hashValues.get("width").getOrElse("1440").toInt
    var height:Int = hashValues.get("height").getOrElse("900").toInt
    val centerStr = hashValues.get("center").getOrElse[String]("{x:102,y:35}")
    val centerHash = JSON.parse(centerStr).asInstanceOf[JSONObject]
    val tile = hashValues.get("tile").getOrElse("-1").toInt
    val filter = hashValues.get("filter").getOrElse("true").toBoolean
//    viewBounds={%22leftBottom%22%20:%20{%22x%22:113.5986328125,%22y%22:39.627685546875},%22rightTop%22%20:%20{%22x%22:115.037841796875,%22y%22:41.06689453125}
    val viewBoundsStr = hashValues.get("viewBounds").getOrElse("")
    var viewBounds:Rectangle2D = null
    if(viewBoundsStr.length > 0){
      val rectObj = JSON.parse(viewBoundsStr).asInstanceOf[JSONObject]
      val leftBottomObj = rectObj.get("leftBottom").asInstanceOf[JSONObject]
      val lettBottom = new Point2D(leftBottomObj.get("x").toString.toDouble,leftBottomObj.get("y").toString.toDouble)
      val rightTopObj = rectObj.get("rightTop").asInstanceOf[JSONObject]
      val rightTop = new Point2D(rightTopObj.get("x").toString.toDouble,rightTopObj.get("y").toString.toDouble)
      viewBounds = new Rectangle2D(lettBottom,rightTop)
    }

    // TODO 支持处理ViewBounds的请求情况

    val x:Double= centerHash.get("x").toString.toDouble
    val y:Double= centerHash.get("y").toString.toDouble
    val start = System.currentTimeMillis()
    val bits:Array[Byte] = this.getImage(scale,new Point2D(x,y),width,height,tile,filter,viewBounds)

    println("getImage cost " + (System.currentTimeMillis() -start))
    response.getOutputStream.write(bits)
  }
}

//class LocalMapImageServerlet extends MapImageServerlet
//{
//  override def getImage(scale:Double, center:Point2D, width:Int, height:Int,tile:Int, filter:Boolean,viewBounds:Rectangle2D):Array[Byte] ={
//
//    val workspace = new Workspace
//      val connection = new WorkspaceConnectionInfo(SparkMapping.WorkspaceFile)
//      workspace.open(connection)
//
//      val map = new com.supermap.mapping.Map(workspace)
//      map.open(SparkMapping.MapName)
//      map.setImageSize(new Dimension(width,height))
//      map.setScale(scale)
//      map.setCenter(center)
//
//      if(viewBounds != null){
//        map.setViewBounds(viewBounds)
//      }
//
//      map.setInflateBounds(true)
//
//
//      if(filter){
//        SparkMapping.setDisplayFilter(map)
//      }
//
//      val bitmap = map.outputMapToBitmap(false)
//
//      val writer = new PNGWriter();
//      val bits = new ByteArrayOutputStream();
//
//      writer.writePNG(bitmap, bits, 0.9F,FilterType.FILTER_NONE);
//
//      map.dispose()
//      workspace.dispose()
//
//      bits.toByteArray()
//  }
//}

//class RDDMapImageServerlet extends MapImageServerlet{
//   override def getImage(scale:Double, center:Point2D, width:Int, height:Int,tile:Int, filter:Boolean,viewBounds:Rectangle2D):Array[Byte] ={
////    SparkMapping.getImage(scale, center, width, height,tile,filter,viewBounds)
//     null
//  }
//}

class RDDHdfsServerlet(m_dmap: DMap) extends MapImageServerlet(m_dmap){
  override def getImage(scale:Double, center:Point2D, width:Int, height:Int,tile:Int, filter:Boolean,viewBounds:Rectangle2D):Array[Byte] ={
    //SparkMapping.getHdfsDataImage(scale, center, width, height,tile,filter,viewBounds)
    val dimension = new Dimension(width, height)
    if(viewBounds != null && !viewBounds.isEmpty){
      m_dmap.outputMapToBitmap(dimension, viewBounds)
    }else if(scale > 0.0 && !center.isEmpty){
      m_dmap.outputMapToBitmap(dimension, scale, center)
    }else{
      null
    }
  }
}

//class RDDHdfsStyleSeverlet extends HttpServlet{
//  private val serialVersionUID: Long = 1L
//
//
//
//  // 浏览器的url中只能是get请求，为了测试方便，这里get 和 post为同样的功能
//  @throws[ServletException]
//  @throws[IOException]
//  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
//    this.doPost(request,response)
//  }
//
//  @throws[ServletException]
//  @throws[IOException]
//  override  protected def doPost(request: HttpServletRequest, response: HttpServletResponse):Unit = {
//    // 参数为图层名称，修改后的前景色，背景色，使用的0xRRGGBB方式的整型值
//    response.setStatus(HttpServletResponse.SC_OK)
//    val args = URLDecoder.decode(request.getQueryString(),"UTF-8").split("&")
//    val hashValues = new scala.collection.mutable.HashMap[String,String]()
//    for( i <- 0 until args.length){
//      val keyAndValue = args(i).split("=");
//      hashValues.put(keyAndValue(0),keyAndValue(1))
//    }
//
//
//    val layerName:String = hashValues.get("layer").getOrElse("1")
//
//    val fC = Integer.valueOf(hashValues.get("foreColor").getOrElse("0x0").replace("0x",""),16)
//    val bC = Integer.valueOf(hashValues.get("backColor").getOrElse("0xFFFFFF").replace("0x",""),16)
//    val itemName:String = hashValues.get("item").getOrElse("城市")
//    val foreColor:Color = new Color(fC)
//    val backColor:Color= new Color(bC)
//    println(layerName,fC,bC,itemName)
//    val status = SparkMapping.setStyle(layerName,itemName,foreColor,backColor)
//    response.setContentType("text")
//    response.getWriter.println(status)
//
//
//  }
//
//}

class StatusSeverlet extends HttpServlet {
  private val serialVersionUID: Long = 1L


  // 浏览器的url中只能是get请求，为了测试方便，这里get 和 post为同样的功能
  @throws[ServletException]
  @throws[IOException]
  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setStatus(HttpServletResponse.SC_OK)
    response.setContentType("text")
    //response.getWriter.println(SparkMapping.StartCost)
  }
}