import java.io.ByteArrayOutputStream
import java.util
import java.util.concurrent.{Executors, TimeUnit}

import com.google.common.cache.{CacheBuilder, LoadingCache}
import com.supermap.bdt.mapping.render.MVTRenderEngine.ThreadPoolSize
import com.supermap.bdt.mapping.render._
import com.supermap.bdt.mapping.util.tiling.CRS
import com.vividsolutions.jts.geom.Geometry
import com.wdtinc.mapbox_vector_tile.adapt.jts.UserDataKeyValueMapConverter
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable.ArrayBuffer

class mvtGeomeryLoader(layerRenders: ArrayBuffer[LayerRender])
  extends com.google.common.cache.CacheLoader[String, java.util.Map[String, java.util.List[Geometry]]] {
  val crs = new CRS(4326)

  override def load(target: String): java.util.Map[String, java.util.List[Geometry]] = {
    //    k为瓦片，需要根据图层分开，
    val (zoom, col, row) = TileDataCache.pathTozxy(target)
    MVTRenderEngine.getTileGeometry(layerRenders, crs, zoom, col, row)
  }
}

class mvtLayerBuilder(layerRenders: ArrayBuffer[LayerRender])
  extends com.google.common.cache.CacheLoader[String, Array[Byte]] {
  /**
    * 根据图层名称异步返回所需要的图层名称
    *
    * @param key
    * @return
    */
  override def load(key: String): Array[Byte] = {
    //    key的内容如 z/x/y.mvt?field=DLBM&value=112&opt===&sourceLayer=DLTB_112@nanning_4326
    val start = System.currentTimeMillis()
    val requests = key.split('?')
    val zxyStr = requests(0)
    val args = if(requests.length > 1) requests(1).split('&') else new Array[String](0)
    val hashValues = new java.util.HashMap[String, String]()
    for (i <- 0 until args.length) {
      val str = args(i)
      val index = str.indexOf('=')
      hashValues.put(str.substring(0, index), str.substring(index + 1))
    }
    val sourceLayerKey = "sourceLayer"
    val innerPointKey = "innerPoint"
    val sourceLayersKey = "sourceLayers"
    var innerPoint = false

    val layerNames: Array[String] = if (hashValues.containsKey(sourceLayerKey)) {
      if (hashValues.containsKey(innerPointKey)) {
        innerPoint = hashValues.get(innerPointKey).toBoolean
      }
      Array(hashValues.get(sourceLayerKey))
    } else if (hashValues.containsKey(sourceLayersKey)) {
      val ls = hashValues.get(sourceLayersKey)
      ls.split(',')
    } else {
      layerRenders.map( l => l.name()).toArray
    }

    var multiThreadCount=0
    val (zoom, col, row) = TileDataCache.pathTozxy(zxyStr)
    val datas = TileDataCache.getTileData(zxyStr)
    var bits: Array[Byte] = new Array[Byte](0)
    if (datas != null) {
      val dataLayer = layerRenders(0)
      val crs = dataLayer.getCRS()
      val userDataConverter = if (!StringUtils.isBlank(dataLayer.m_idFieldName)) {
        new UserDataKeyValueMapConverter(dataLayer.m_idFieldName)
      } else {
        new UserDataKeyValueMapConverter()
      }

      val queryBounds = MapRender.getQueryBounds(crs, zoom, col, row)
      var maxObjCount = 0;
      val curentThreadRuners = new ArrayBuffer[MvtEncodingRunner]()
      val multiThreadRunnders = new ArrayBuffer[MvtEncodingRunner]()
      //    判断是否需要多线程，如果大于4层，且对象数大于3000前，使用多线程
      val MaxObjCount = 800

        layerNames.foreach(layerName => {
          if (datas.containsKey(layerName)) {
            val geos = datas.get(layerName)
            val runner = if (innerPoint) {
              new InernalPointEncodingRunner(layerName, geos, queryBounds, userDataConverter);
            } else {
              new MvtEncodingRunner(layerName, geos, queryBounds, userDataConverter);
            }
            if (geos.size() > MaxObjCount) {
              multiThreadRunnders += runner
            } else {
              curentThreadRuners += runner
            }
          }
        })


      val ThreadPoolSize = 4

      if(multiThreadRunnders.length > 0 && curentThreadRuners.length > 0){
//        使用多线程完成处理
        var poolSize = multiThreadRunnders.length;
        if(poolSize > ThreadPoolSize){
          poolSize = ThreadPoolSize
        }
        val executor = Executors.newFixedThreadPool(poolSize)
//        更多的数据由多线程来完成
        multiThreadRunnders.foreach(run => executor.submit(run))
        multiThreadCount = poolSize
        executor.shutdown();

//        当前线程的使用当前线程完成
        curentThreadRuners.foreach(run => run.run())

        while (!executor.isTerminated){
          Thread.sleep(50)
        }

      }else{
//        使用当前线程完成处理
        val invokeFun = (run:MvtEncodingRunner) => run.run()
        curentThreadRuners.foreach(invokeFun)
        multiThreadRunnders.foreach(invokeFun)
      }

      val dataLayerWriter = new ByteArrayOutputStream()
      curentThreadRuners.foreach( run => dataLayerWriter.write(run.getMvtBitDatas()))
      multiThreadRunnders.foreach( run => dataLayerWriter.write(run.getMvtBitDatas()))
      bits = dataLayerWriter.toByteArray
      dataLayerWriter.close()
    }


    val cost = System.currentTimeMillis() - start
    val threadid = Thread.currentThread().getName
    if(cost > 1000){
      println(s"thread $threadid. threadCount $multiThreadCount build $key cost $cost")
    }
    bits
  }
}

object TileDataCache {
  //  用于表示该行列号的瓦片是否请求过
  private var g_mvtGeosCache: LoadingCache[String, java.util.Map[String, java.util.List[Geometry]]] = null
  private var g_mvtCache: LoadingCache[String, Array[Byte]] = null


  def initTileLoader(layerRenders: ArrayBuffer[LayerRender]): Unit = {
    val loader = new mvtGeomeryLoader(layerRenders)
    g_mvtGeosCache = CacheBuilder.newBuilder()
      .maximumSize(300)
      .expireAfterAccess(60, TimeUnit.MINUTES)
      .build(loader)

    val mvtBuilder = new mvtLayerBuilder(layerRenders)
    g_mvtCache = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(60, TimeUnit.MINUTES)
      .build(mvtBuilder)
  }

  def getTile(zoom: Int, col: Int, row: Int, tileKey: String): Array[Byte] = {
    val zxyUrl = s"$zoom/$col/$row"
    g_mvtCache.get(tileKey)
  }

  def getTileData(zxyurl: String): java.util.Map[String, java.util.List[Geometry]] = {
    g_mvtGeosCache.get(zxyurl)
  }

  def pathTozxy(path: String): (Int, Int, Int) = {
    val subs = path.split('/')
    val levelStr = subs(0)
    val columnStr = subs(1)
    val rowStr = subs(2)

    (levelStr.toInt, columnStr.toInt, rowStr.toInt)

  }

}
