package com.supermap.rasterserver

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
import geotrellis.spark.io.hbase._
import org.apache.hadoop.hbase.HBaseConfiguration

import scala.concurrent._

object RasterServerHBase extends App with Service {

  val argsStrings = args
  // Create a reader that will read in the indexed tiles we produced in Ingest.
  val instance = HBaseInstance(HBaseConfiguration.create())
  val attributeStore = HBaseAttributeStore(instance)
  val valueReader:OverzoomingValueReader = HBaseValueReader(attributeStore)

  def reader(layerId: LayerId) = valueReader.reader[SpatialKey, MultibandTile](layerId)

  //  val slopeCM = ColorMap((0.0 to 45.0 by 0.5).toArray, ColorRamps.GreenToRedOrange)
  //  val slopeCCM = ColorMap((15.0 to 45.0 by 0.1).toArray, ColorRamps.GreenToRedOrange)
  //  val demBJ = ColorMap((-121 to 2202 by 1).toArray, ColorRamps.Plasma)
  //  val demCM = ColorMap((-100 to 8805 by 1).toArray, ColorRamps.Inferno)
  //  val reclass = ColorMap((1 to 6 by 1).toArray, ColorRamps.BlueToOrange)

  override implicit val system = ActorSystem("tutorial-system")
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(root, "172.16.18.8", 8090)
}

trait Service {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer
  val logger: LoggingAdapter

  def pngAsHttpResponse(png: Png): HttpResponse ={
    HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`image/png`), png.bytes))
  }

  def root =
    pathPrefix(Segment / IntNumber / IntNumber / IntNumber) { (render, zoom, x, y) =>
      complete {
        Future {

          render match {

            case "test4326" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServerHBase.reader(LayerId("F4_4326", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.renderPng()
                pngAsHttpResponse(png)
              }
            case "test3857" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServerHBase.reader(LayerId("F4_3857", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>
                    None
                }
              tileOpt.map { tile =>
                // Render as a PNG
                val png = tile.renderPng()
                pngAsHttpResponse(png)
              }
            case "imageChina" =>
              // Read in the tile at the given z/x/y coordinates.
              val tileOpt: Option[MultibandTile] =
                try {
                  Some(RasterServerHBase.reader(LayerId("imageChina", zoom)).read(x, y))
                } catch {
                  case _: ValueNotFoundError =>{
                    println((zoom,x,y,"not exists"));
                    None
                  }
                }

              if(tileOpt.isEmpty){
                println("")
                Some(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`image/png`), new Array[Byte](0))));
//                None;
              }else {
                tileOpt.map(tile => {
                  // Render as a PNG
                  val png = tile.renderPng()
                  pngAsHttpResponse(png)
                });
              }
          }
        }
      }
    } ~
      pathPrefix(Segment / Segment) { (dir, fileName) => {
        getFromFile(dir + "/" + fileName);
      }
      } ~
      pathPrefix(Segment / Segment / Segment) { (dir, subdir, fileName) => {
      	println((dir, subdir, fileName));
        getFromFile(dir + "/" + subdir + "/" + fileName);
      }
      }
}
