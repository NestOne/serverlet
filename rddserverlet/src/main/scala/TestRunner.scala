import com.supermap.bdt.mapping.dmap.{DLayerDataHBase, DMap}
import com.supermap.bdt.io.geomesahbase.GeoMesaHBaseReader
import com.supermap.bdt.mapping.util.tiling.CRS
import com.supermap.data.Workspace
import org.apache.spark.{SparkConf, SparkContext}

object TestRunner {
  def main(args: Array[String]): Unit = {
//    val sparkConf = new SparkConf().setAppName("AnalyzeResultMVTBuildTest")
//    if (!sparkConf.contains("spark.master")) {
//      sparkConf.setMaster("local[*]")
//    }
//
//    sparkConf.set("spark.local.dir", "G:\\Cache")
//    sparkConf.set("spark.network.timeout", "300")
//
//    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
//    sparkConf.set("spark.kryo.registrator", "org.locationtech.geomesa.spark.GeoMesaSparkKryoRegistrator")
//
//    val sparkContext = new SparkContext(sparkConf)

     //GeoMesaHBaseReader.read(sparkContext, "nanning", "DLTB_1_2w_9")



    val dMap = new DMap()
    dMap.initialize("F:/nanning/nanning_3857/nanning.smwu", "DLTB_2w_Double@nanning")
    val dLayerData = DLayerDataHBase("nanning", "DLTB_1_2w_9", "", null)
    dLayerData.setPrj(CRS(3857))
    //dLayerData.readFeatureRDD(9, 110, 25)

    dMap.addDLayer(dLayerData, 0, "test")

    dMap.renderBitmap(9, 110, 25)
  }
}
