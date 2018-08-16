import com.supermap.bdt.mapping.dmap.{DMap}

import util.Arguments

object DMapBuildRun {
  def main(args: Array[String]): Unit = {

    println("arguments : " + args.mkString("&&"))

    val start = System.currentTimeMillis()

    val arguments = Arguments(args)

    val wkPath = arguments.get("workspace").getOrElse({
      println("need arg 'workspace' like -workspace=nanning.smwu")
      return })

    val mapname = arguments.get("mapname").getOrElse({
      println("need arg 'mapname' like -mapname=DLTB_2w_Double@nanning")
      return })

    val buildcfg = arguments.get("buildcfg").getOrElse({
      println("need arg 'buildcfg' like -buildcfg=hdfs:///test (HDFS) or -buildcfg=192.168.1.2:2181 (HBase-zookeeper)")
      return })

    val dMap = new DMap()
    dMap.initialize(wkPath, mapname)

    dMap.build(buildcfg, true)

    dMap.close()

    println("dMap build Cost: " + (System.currentTimeMillis() - start) + "ms")
  }
}
