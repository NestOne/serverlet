package util


import java.io.{File, FilenameFilter}

import com.supermap.bdt.io.mongo.{MongoConnectionInfo, MongoWriter}
import com.supermap.bdt.io.sdx.{DSConnectionInfo, SDXReader}
import com.supermap.data._
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/10/30.
  */
object UDBToMongo {
  def main(args:Array[String]):scala.Unit ={

    // 将UDB中的数据，复制到mongodb中，希望能够跳过不正确的数据
    val m_sparkConf = new SparkConf().setAppName("RddMappingServer")
    if (!m_sparkConf.contains("spark.master")) {
      m_sparkConf.setMaster("local[*]")
    }
    m_sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val m_sparkContext = new SparkContext(m_sparkConf)

    // 遍历udb，导入点、线对象
    val dataPath = args(0)
    val dataFile = new File(dataPath)

    val udbs = new ArrayBuffer[DSConnectionInfo]()
    val dataType=args(1)
    val mongoServer=args(2)
    val mongoDatabase=args(3)
    val targetDataset=args(4)
    if(dataFile.isFile && dataFile.exists()){
      udbs.append(DSConnectionInfo.UDB(dataPath))
    }else{
      val subDirs = subdirs(dataFile)
      val udbFiles = dataFile.listFiles(new udbFilter())
      for(file <- udbFiles){
        println(file.getAbsolutePath)
        udbs.append(DSConnectionInfo.UDB(file.getAbsolutePath))
      }
      for(d <- subDirs){
        val udbFiles = d.listFiles(new udbFilter())
        for(file <- udbFiles){
          println(file.getAbsolutePath)
          udbs.append(DSConnectionInfo.UDB(file.getAbsolutePath))
        }
      }
    }
    val dtType =  dataType match {
      case "point" => DatasetType.POINT
      case "line" =>DatasetType.LINE
      case "region" =>DatasetType.REGION
      case _ =>DatasetType.POINT
    }
    udbs.foreach( dsInfo =>{

      // 打开获取数据集名称
      val udbFileName = dsInfo.server
      val udbds = new Datasource(EngineType.UDB)
      udbds.open(new DatasourceConnectionInfo(udbFileName,"ds_alias",""))
      val datasets = udbds.getDatasets()
      var dtName:String = null
      for(i <- 0 until datasets.getCount if dtName == null){
        if(datasets.get(i).getType == dtType){
          dtName = datasets.get(i).getName
        }
      }
      udbds.close()
      println("start import " + dsInfo.server + " " + dtName)
      var connet: immutable.Map[DSConnectionInfo, Array[String]] = immutable.Map[DSConnectionInfo, Array[String]]()
      connet += (dsInfo -> Array(dtName))
      val dtRdd = SDXReader.readFromDSArray(m_sparkContext, connet, m_sparkContext.defaultParallelism, isDriver = false)
      val mongoConnect = new MongoConnectionInfo(mongoServer,mongoDatabase)
     // MongoWriter.GeometryFormat = "geojson"
      MongoWriter.write(dtRdd,mongoConnect,targetDataset,null)
      println("import " + dsInfo.server + " " + dtName+" done.")
    })

  }

  def subdirs(dir:File):Iterator[File] ={
    val children = dir.listFiles.filter(_.isDirectory)
    children.toIterator ++ children.toIterator.flatMap(subdirs(_))
  }
}

class udbFilter extends FilenameFilter {
  override def accept(dir: File, name: String): Boolean = {
    name.endsWith(".udb")
  }
}
