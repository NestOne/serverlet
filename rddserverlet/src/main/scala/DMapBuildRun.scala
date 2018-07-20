import com.supermap.bdt.mapping.dmap.{DLayerDataHBase, DMap}
import com.supermap.bdt.io.geomesahbase.GeoMesaHBaseReader
import com.supermap.bdt.mapping.util.tiling.CRS
import com.supermap.data.Workspace
import org.apache.spark.{SparkConf, SparkContext}

object DMapBuildRun {
  def main(args: Array[String]): Unit = {

    val arguments = Arguments(args)

    val wkPath = arguments.get("workspace").getOrElse({
      println("need arg 'workspace' like -workspace=nanning.smwu")
      return })

    val mapname = arguments.get("mapname").getOrElse({
      println("need arg 'mapname' like -mapname=DLTB_2w_Double@nanning")
      return })

    val buildcfg = arguments.get("buildcfg").getOrElse({
      println("need arg 'buildcfg' like -buildcfg=hdfs:///test (HDFS) or buildcfg=192.168.1.2:2181 (HBase-zookeeper)")
      return })

    val dMap = new DMap()
    dMap.initialize(wkPath, mapname)

    dMap.build(buildcfg, true)


    dMap.close()
  }
}
