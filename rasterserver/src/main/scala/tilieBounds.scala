object tilieBounds {

  def main(args:Array[String]):Unit ={

//  left,bottom,right,top
    val indexBounds = Array(-180.0,-90.0,180.0,90.0);
    val bounds = Array(114.99986111111112,38.99958333333456,118.00041666666654,42.00013888888889);
//    第0及均为一张瓦片，然后计算出bounds对应层级所对应的索引范围
    var tileResolution = indexBounds(2) - indexBounds(0);

//    TMS的行列号计算方式
    for( i <- 0 until 10){
      val xMin = ((bounds(0) - indexBounds(0))/tileResolution).toInt;
      val xMax = ((bounds(2) - indexBounds(0))/tileResolution).toInt;
      val yMin = ((bounds(1) - indexBounds(1))/tileResolution).toInt;
      val yMax = ((bounds(3) - indexBounds(1))/tileResolution).toInt;
      tileResolution /= 2.0;
      println("level:"+i+",minkey:"+(xMin,yMin)+",maxKey:"+(xMax,yMax));
    }
    //    SuperMap, WMTS的行列号计算方式

  }

}
