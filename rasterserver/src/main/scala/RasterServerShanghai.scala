package com.supermap.rasterserver

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import akka.actor._
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import geotrellis.raster.histogram.Histogram
import spray.json.DefaultJsonProtocol._
import geotrellis.raster.io._

import scala.concurrent._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration

object RasterServerShanghai extends App with ServiceShanghai {

  val configuration = new Configuration()
  // Create a reader that will read in the indexed tiles we produced in Ingest.
  val hadoopValueReader
  = HadoopValueReader(new Path("hdfs://172.16.18.8:9000/liufang/catalog/"), configuration)
  def reader(layerId: LayerId)
  = hadoopValueReader.reader[SpatialKey, MultibandTile](layerId)

  val attributeStore = hadoopValueReader.attributeStore

  val ndviRamp = ColorRamp(
    0xFFFFFFFF, 0xC8C8C8FF, 0x783700FF, 0x860600FF,
    0xC14C00FF, 0xEFC306FF, 0x71A63EFF, 0x07893BFF,
    0x006B27FF, 0x006B27FF, 0x004519FF, 0x004519FF)
  val ndviColor = ColorMap((-1.0 to 1.0 by 0.01).toArray, ndviRamp)

  val ndviReclass = ColorRamp(
    0xEFB406FF, 0x71A63EFF, 0x07893BFF, 0x004519FF)
  val reclassColor = ColorMap((1 to 4 by 1).toArray, ndviReclass)

  val ndwiRamp = ColorRamp(
    0x8C3200FF, 0xBE5000FF, 0xE16F14FF, 0xF5C846FF,
    0x46B464FF, 0x4678C8FF, 0x3264B4FF, 0x1E50A0FF,
    0x0A3C8CFF, 0x002878FF)
  val ndwiColor = ColorMap((-1.0 to 1.0 by 0.01).toArray, ndwiRamp)

  override implicit val system = ActorSystem("tutorial-system")
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(root, "172.16.18.8", 8099)
}

trait ServiceShanghai {
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

            case "ndwiR" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServerShanghai.reader(LayerId("shanghai_MUL", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                val greenBand = tile.band(1).convert(DoubleUserDefinedNoDataCellType(-9999))
                val nirBand = tile.band(5).convert(DoubleUserDefinedNoDataCellType(-9999))

                val subBand = greenBand - nirBand
                val addBand = greenBand + nirBand

                val ndwi = subBand / addBand
                // Render as a PNG
                val png = ndwi.renderPng(RasterServerShanghai.ndwiColor)
                pngAsHttpResponse(png)
              }
            case "ndviR" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServerShanghai.reader(LayerId("shanghai_MUL", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                val redBand = tile.band(3).convert(DoubleUserDefinedNoDataCellType(-9999))
                val nirBand = tile.band(5).convert(DoubleUserDefinedNoDataCellType(-9999))

                val subBand = nirBand - redBand
                val addBand = nirBand + redBand

                val ndvi = subBand / addBand
                // Render as a PNG
                val png = ndvi.renderPng(RasterServerShanghai.ndviColor)
                pngAsHttpResponse(png)
              }
			  case "ndvi" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServerShanghai.reader(LayerId("shanghai_NDVI", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServerShanghai.ndviColor)
                pngAsHttpResponse(png)
              }
			  case "ndvi_reclass" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServerShanghai.reader(LayerId("ndvi_reclass", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.band(0).renderPng(RasterServerShanghai.reclassColor)
                pngAsHttpResponse(png)
              }
          }
        }
      }
    } ~
    pathEndOrSingleSlash {
      getFromFile("static/index_ol.html")
    } ~
    pathPrefix("") {
      getFromDirectory("static")
    }
}
