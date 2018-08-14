package rasterserver

import akka.actor._
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import scala.concurrent._

object RasterServer extends App with Service121 {


  val configuration = new Configuration()
  // Create a reader that will read in the indexed tiles we produced in Ingest.
  val hadoopValueReader:OverzoomingValueReader = HadoopValueReader(new Path("hdfs://bigdata1:9000/home/zhangnj/catalog"), configuration)

  def reader(layerId: LayerId) = hadoopValueReader.reader[SpatialKey, MultibandTile](layerId)

  val hadoopValueReaderBJ:OverzoomingValueReader = HadoopValueReader(new Path("hdfs://bigdata1:9000/0514/catalog"), configuration)

  def readerBJ(layerId: LayerId) = hadoopValueReaderBJ.reader[SpatialKey, MultibandTile](layerId)

  val hadoopValueReaderHN:OverzoomingValueReader = HadoopValueReader(new Path("hdfs://bigdata1:9000/liufang/catalog"), configuration)

  def readerHN(layerId: LayerId) = hadoopValueReaderHN.reader[SpatialKey, MultibandTile](layerId)

  val slopeCM = ColorMap((0.0 to 45.0 by 0.5).toArray, ColorRamps.GreenToRedOrange)
  val slopeCCM = ColorMap((15.0 to 45.0 by 0.1).toArray, ColorRamps.GreenToRedOrange)
  val demBJ = ColorMap((-121 to 2202 by 1).toArray, ColorRamps.Plasma)
  val demCM = ColorMap((-100 to 8805 by 1).toArray, ColorRamps.Inferno)
  val reclass = ColorMap((1 to 6 by 1).toArray, ColorRamps.BlueToOrange)

  override implicit val system = ActorSystem("tutorial-system")
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(root, "localhost", 8090)
}

trait Service121 {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer
  val logger: LoggingAdapter

  def pngAsHttpResponse(png: Png): HttpResponse =
    HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`image/png`), png.bytes))

  def root =
    pathPrefix(Segment / IntNumber / IntNumber / IntNumber) { (render, zoom, x, y) =>
      complete {
        Future {

          render match {
            case "slopeRealTime" =>

              val mataData: TileLayerMetadata[SpatialKey] = RasterServer.
                hadoopValueReader.attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](LayerId("DEMchina_TT", zoom))
              val cellSize = mataData.layout.cellSize
              val keyBounds = mataData.bounds

              val cropedTileOption: Option[Tile] =
                try {
                  if (keyBounds.includes(SpatialKey(x, y))) {
                    val tiles =
                      for (i <- -1 to 1; j <- -1 to 1)
                        yield {
                          val col = x + i
                          val row = y + j
                          if (keyBounds.includes(SpatialKey(col, row))) {
                            (SpatialKey(col, row), RasterServer.reader(LayerId("DEMchina_TT",
                              zoom)).read(col, row).band(0))
                          } else {
                            (SpatialKey(col, row), ArrayTile.empty(mataData.cellType, 256, 256))
                          }
                        }

                    val stitched = tiles.stitch()
                    val slopedTile = stitched.slope(cellSize)

                    Some(slopedTile.crop(256, 256, 511, 511))
                  }
                  else {
                    None
                  }
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }

              cropedTileOption.map { cropedTile =>
                val png: Png = cropedTile.renderPng(RasterServer.slopeCM)
                pngAsHttpResponse(png)
              }

            case "dem" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServer.reader(LayerId("DEMchina_TT", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServer.demCM)
                pngAsHttpResponse(png)
              }
            case "slope" =>
              // Read in the tile at the given z/x/y coordinates.
              val slopeTile: Option[MultibandTile] =
                try {
                  Some(RasterServer.reader(LayerId("DEMchina_FF_slopeTT", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              slopeTile.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServer.slopeCM)
                pngAsHttpResponse(png)
              }
            case "mathCon" =>
              // Read in the tile at the given z/x/y coordinates.
              val slopeTile: Option[MultibandTile] =
                try {
                  Some(RasterServer.reader(LayerId("DEMchina_slope_mathTT", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              slopeTile.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServer.slopeCCM)
                pngAsHttpResponse(png)
              }
            case "clip" =>
              // Read in the tile at the given z/x/y coordinates.
              val slopeTile: Option[MultibandTile] =
                try {
                  Some(RasterServer.reader(LayerId("DEMchina_FF_clip_TT", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              slopeTile.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServer.demCM)
                pngAsHttpResponse(png)
              }
            case "beijingDEM" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServer.readerBJ(LayerId("beijingDEM", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServer.demBJ)
                pngAsHttpResponse(png)
              }
            case "beijingReclass" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServer.readerBJ(LayerId("beijingClip", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServer.reclass)
                pngAsHttpResponse(png)
              }
            case "hainan" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServer.readerHN(LayerId("hainanF", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.renderPng()
                pngAsHttpResponse(png)
              }
            case "hainanF" =>

              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServer.readerHN(LayerId("hainanF", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.renderPng()
                pngAsHttpResponse(png)
              }
            case "4490" =>
              val tileOpt: Option[MultibandTile] =
                try {
                  val data = RasterServer.readerBJ(LayerId("BJprj4326", zoom));
                  val tile = data.read(x, y);
                  Some(tile);
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServer.demBJ)
                pngAsHttpResponse(png)
              }

          }
        }
      }
    } ~
      pathPrefix(Segment / Segment) { (dir,fileName) => {
        getFromFile(dir + "/" +fileName);
      }
      }~
      pathPrefix(Segment / Segment /Segment) { (dir,subdir, fileName) => {
        getFromFile(dir + "/" + subdir +"/" +fileName);
      }
      }
}
