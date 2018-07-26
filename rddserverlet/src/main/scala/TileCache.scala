import java.util
import java.util.Collections
import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, LoadingCache}
import com.supermap.bdt.mapping.render.{LayerRender, MVTRenderEngine, MapRender}
import com.supermap.bdt.mapping.util.tiling.CRS

import scala.collection.mutable.ArrayBuffer

class mvtLoader(layerRenders : ArrayBuffer[LayerRender]) extends com.google.common.cache.CacheLoader[String,java.util.Map[String,Array[Byte]]]{
  val mvtRender = new MVTRenderEngine
  val crs = new CRS(4326)

  override def load(target: String): java.util.Map[String,Array[Byte]] = {
//    k为瓦片，需要根据图层分开，
    val (tileSource, zoom, col, row) = pathToURL(target)

    val bytes = generatemvt(tileSource,zoom,col,row,"")
    val result = new util.HashMap[String,Array[Byte]]();
    result.put("all",bytes);
    result
  }

  def pathToURL(path : String) : (String,Int, Int, Int)={
    val subs = path.split("/")
    val levelStr = subs(subs.length - 3)
    val columnStr = subs(subs.length - 2)

    var rowStr = subs(subs.length - 1)
    rowStr = rowStr.substring(0, rowStr.length - 4)
    val tilesFolder = subs(subs.length - 4)

    (tilesFolder,levelStr.toInt, columnStr.toInt, rowStr.toInt)



  }

  def generatemvt(tileSource:String,zoom:Int,col:Int,row:Int, key:String):Array[Byte]={
    val start = System.currentTimeMillis()
    var result:Array[Byte] = null

    if(tileSource.equals("tiles")){
      val mvtRender = new MVTRenderEngine
      val queryBounds = MapRender.getQueryBounds(crs, zoom, col, row)
      mvtRender.initialize(queryBounds)
      layerRenders.foreach(l => {
        l.addMvtLayer(zoom, col, row, mvtRender, queryBounds)
      })
      result = mvtRender.build()
    }else{
      val render = layerRenders.find( l => {
        tileSource.equals(l.name)
      } )
      val mvtRender = new MVTRenderEngine
      val queryBounds = MapRender.getQueryBounds(crs, zoom, col, row)
      mvtRender.initialize(queryBounds)
      render.get.addMvtLayer(zoom, col, row, mvtRender, queryBounds)
      result = mvtRender.build()
    }
    val cost = System.currentTimeMillis() - start
    println(s"$tileSource/$zoom/$col/$row.mvt cost(ms) $cost")

    //    TileCache.putTile()
    result
  }
}
/**
  * 提供mvt瓦片的动态缓存能力，使用HDFS API 以便可以实现动态缓存能力，确保并发性能
  */
object TileCache {
//  用于表示该行列号的瓦片是否请求过
  private var  g_doneCache: LoadingCache[String, java.util.Map[String,Array[Byte]]] = null

  def initTileLoader(layerRenders : ArrayBuffer[LayerRender]):Unit ={
    val loader = new mvtLoader(layerRenders)
    g_doneCache = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(60, TimeUnit.MINUTES)
      .build(loader);
  }
  def getTile(url:String):java.util.Map[String,Array[Byte]]={
    return g_doneCache.get(url)
  }



}
