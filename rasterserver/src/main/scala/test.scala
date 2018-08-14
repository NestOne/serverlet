import geotrellis.raster._
import geotrellis.raster.render.{ColorMap, ColorRamps}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hbase.{HBaseAttributeStore, HBaseInstance, HBaseValueReader}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.TableNotFoundException

object readmetadata {

  def getLocalReader():OverzoomingValueReader ={
    val configuration = new Configuration()
    new Path("hdfs://bigdata1:9000/liufang/catalog")
    HadoopValueReader(new Path("file:///M:/gisdata/geotrellis/catalog"), configuration)
  }

  def getHBaseReader():OverzoomingValueReader ={
    val configuration = new Configuration
    configuration.set("hbase.zookeeper.quorum", "192.168.12.201:2181,192.168.12.202:2181,192.168.12.203:2181")
    configuration.set("hbase.rootdir", "hdfs://192.168.12.121:9000/hbase")
    val instance = HBaseInstance(configuration)
    val attributeStore = HBaseAttributeStore(instance)
    HBaseValueReader(attributeStore)
  }
  //  获取某个目录下所有的缓存信息
  def main(args: Array[String]): Unit = {



    val dirReader:OverzoomingValueReader = getHBaseReader()

    val allLyaers = dirReader.attributeStore.layersWithZoomLevels
    allLyaers.foreach(layerAndZooms => {
      var name = layerAndZooms._1
      val zooms = layerAndZooms._2.sortBy[Int](v => v)
      var minZoom = -1
      val maxZoom = zooms.last
      var layerAttribute:TileLayerMetadata[SpatialKey] = null
      for (z <- zooms if minZoom == -1) {
        try {
          layerAttribute = dirReader.attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](LayerId(name, z))
          minZoom = z
          println((name,layerAttribute.crs.epsgCode.get))
        } catch {
          case _: AttributeNotFoundError => println("the zoom " + z + " of layer " + name + "does not exist.")
        }
      }

//      计算分辨率
      val extent = layerAttribute.layoutExtent
      println(layerAttribute.layoutExtent.xmin, layerAttribute.layoutExtent.ymax)
      println(layerAttribute.extent)
      val tileLayout = layerAttribute.tileLayout


      val getRes = (layer:TileLayerMetadata[SpatialKey]) =>{
        val extent1 = layer.layoutExtent
        val xR = (extent1.xmax-extent1.xmin)/layer.tileCols/layer.layoutCols
        val yR = (extent1.ymax-extent1.ymin)/layer.tileRows/layer.layoutRows
        if(xR > yR) xR else yR
      }

      val last = dirReader.attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](LayerId(name, maxZoom))
      val cellType = layerAttribute.cellType

      println(cellType.bits)
      val minZR = getRes(layerAttribute)
      val maxZR = getRes(last)

      try{
        val reader = dirReader.reader[SpatialKey, MultibandTile](LayerId(name, minZoom))
        val tile = reader.read(layerAttribute.bounds.get.minKey)
        println((name,tile.bandCount))
      }catch {
//        case e:TableNotFoundException => e.printStackTrace()
        case e:Exception => e.printStackTrace()
      }



      println(("resolution",(maxZoom-minZoom),minZR/maxZR, Math.log((minZR/maxZR))/Math.log(2)))
    })

    //"col":209,"row":34
//    val reader = dirReader.reader[SpatialKey, MultibandTile](LayerId("BJ4326", 8))
//    val tile = reader.read(209, 34)
//    val demBJ = ColorMap((-121 to 2202 by 1).toArray, ColorRamps.Plasma)
//    val png = tile.band(0).renderPng()
//    png.write("result.png")

  }
}
