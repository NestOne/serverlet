import com.supermap.data.CoordSysTranslator;
import com.supermap.data.Point2D;
import com.supermap.data.Point2Ds;
import com.supermap.data.PrjCoordSys;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 用于根据sci生成对应的html页面，以及将jar包中的js文件解压到sci文件的跟目录下，方便对不同投影的支持
 */
public class HtmlGenerator {

    private static String REPLACEKEY="<!--inserjscode-->";
    private static double[] MAPBOX_INDEXBOUNDS=new double[]{-20037508.342789199,-20037508.342789199,20037508.342789199,20037508.342789199};

    /**
     * 将资源中的所有css, js文件保存到folder的./dist目录下
     * @param folder
     */
    public static void saveAsJS(String folder) throws Exception {
        ClassLoader loader =HtmlGenerator.class.getClassLoader();
        extractFiles(folder, loader, "dist");
        extractFiles(folder, loader, "editor");
    }

    private static void findFiles(String folder,ClassLoader loader, List<String> filenames) throws Exception{
        BufferedReader in = new BufferedReader(new InputStreamReader(loader.getResourceAsStream(folder)));
        String resource;
        while( (resource = in.readLine()) != null ) {
            String resName =folder+"/"+resource;
            boolean dir = new File(loader.getResource(resName).toURI()).isDirectory();
            if(!dir){
                filenames.add(resName);
            }else{
                findFiles(resName,loader,filenames);
            }
        }
    }

    private static void extractFiles(String folder, ClassLoader loader, String distFolder) throws Exception {
        String classResourceName = HtmlGenerator.class.getName().replace(".", "/") + ".class";
        URL classResourceURL = loader.getResource(classResourceName);
        String classResourcePath = classResourceURL.getPath();
        List<String> filenames = new ArrayList<>();

        if (classResourceURL.getProtocol().equals("file")) {
            // 开发环境里class和resource同位于target/classes目录下
            findFiles(distFolder,loader,filenames);

        } else if (classResourceURL.getProtocol().equals("jar")) {
            // 打包成jar包时,class和resource同位于jar包里
            String jarPath = classResourcePath.substring(classResourcePath.indexOf("/"), classResourceURL.getPath()
                    .indexOf("!"));
            try {
                JarFile jarFile = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
                Enumeration jarEntries = jarFile.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
                    String resourceName = jarEntry.getName();
                    if (resourceName.startsWith(distFolder) && !jarEntry.isDirectory()) {
                        filenames.add(resourceName);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }

//        另存文件,如果文件存在跳过，以免覆盖用户数据
        File targetDir = new File(folder+"/"+distFolder);
        if(!targetDir.exists()){
            targetDir.mkdir();
        }
        for(String file: filenames){
            File targetFile = new File(folder+"/"+file);
            if(!targetFile.exists()){

                DataInputStream  reader = new DataInputStream(loader.getResourceAsStream(file));
                if(!targetFile.getParentFile().exists()){
                    targetFile.getParentFile().mkdir();
                }
                FileOutputStream writer = new FileOutputStream(targetFile);
                byte[] bytes = new byte[4096];
                int bitCount=0;
                while ((bitCount = reader.read(bytes)) != -1){
                    writer.write(bytes,0,bitCount);
                }

                reader.close();
                writer.close();
            }


        }
    }


    private  static String readLines(String resouceName) throws Exception {
        ClassLoader loader =HtmlGenerator.class.getClassLoader();
        BufferedReader in = new BufferedReader(new InputStreamReader(loader.getResourceAsStream(resouceName),"UTF-8"));
        StringBuffer buffer = new StringBuffer();
        String line = "";
        while ((line = in.readLine()) != null){
            buffer.append(line+"\n");
        }
        return  buffer.toString();
    }

    public static double[] alterMBGLCenter(double centerX, double centerY, double[] indexbounds, int tileSize){
        //        进行投影转换
        double cellSize = (indexbounds[2] - indexbounds[0])/tileSize;
        double pixelX = (centerX-indexbounds[0])/cellSize;
        double pixelY= (indexbounds[3] - centerY)/cellSize;

//        该像素点在3857坐标系下的坐标值
        double mapboxCellSize = (MAPBOX_INDEXBOUNDS[2] - MAPBOX_INDEXBOUNDS[0])/tileSize;
        double mbX=MAPBOX_INDEXBOUNDS[0] + pixelX*mapboxCellSize;
        double mbY=MAPBOX_INDEXBOUNDS[3] - pixelY*mapboxCellSize;

//        需要转换成经纬度
        Point2Ds point2ds = new Point2Ds(new Point2D[]{new Point2D(mbX,mbY)});
        PrjCoordSys prj3857 = PrjCoordSys.fromEPSG(3857);
        CoordSysTranslator.inverse(point2ds,prj3857);
        mbX=point2ds.getItem(0).getX();
        mbY=point2ds.getItem(0).getY();

        return new double[]{mbX,mbY};
    }

    /**
     * 将jar文件中的dist目录，已经重新生成index.html页面到sci文件的同级目录下，默认为ol4的查看页面
     */
    public static void generateOL4(String folder, String cacheName,int epsgcode, int minZoom, int maxZoom,
                                   double[] resolutions, double centerX, double centerY, double[] indexbounds, int tileSize) throws Exception {


//        提取dist目录文件的内容到folder中
        saveAsJS(folder);

        String olbaseContent = readLines("olbase.html");
        int zoom = minZoom;
//        生成分辨率代码， var sss=[...];
        StringBuffer ressbuffer = new StringBuffer();
        ressbuffer.append('[');
        for(double d : resolutions){
            ressbuffer.append(d);
            ressbuffer.append(',');
        }
        ressbuffer.setCharAt(ressbuffer.length()-1,']');
//        初始化地图
        StringBuffer iniMap = new StringBuffer();
        String orginalLine = "";
        //        预定义投影直接使用此前的设置
        if(epsgcode == 3857 || epsgcode == 4326){
            iniMap.append(String.format("var proj = ol.proj.get('EPSG:%d');\n",epsgcode));
        }else {
//            自定义投影
            if(epsgcode == 0){
                epsgcode = -1000;
            }
            iniMap.append("var proj= new ol.proj.Projection({\n" +
                    "        code:'EPSG:"+epsgcode+"',\n" +
                    String.format("        extent:[%f,%f,%f,%f]\n",indexbounds[0],indexbounds[1],indexbounds[2],indexbounds[3]) +
                    "      });\n");
        }
        iniMap.append("    var ress="+ressbuffer.toString()+";\n");
        iniMap.append(String.format("    var center = [%f, %f];\n", centerX, centerY));
        iniMap.append(String.format("    var zoom=%d;\n", minZoom));
        iniMap.append(String.format("    var maxZ=%d;\n", maxZoom));
        iniMap.append(String.format("    var mapName='%s';\n", cacheName));
        iniMap.append(String.format("    var tileSize=%d;\n", tileSize));

//        将iniMap字符串替换到olbase.html页面中
        olbaseContent = olbaseContent.replace(REPLACEKEY,iniMap.toString());
        String indexFile = folder+"/index.html";
        if(!new File(indexFile).exists()){
            OutputStreamWriter writer  = new OutputStreamWriter(new FileOutputStream(indexFile),"UTF-8");
            writer.write(olbaseContent);
            writer.close();
        }

//        生成完ol的页面后，如果是3857, 测处理mapbox-gl页面
//        mapboxgl 本身没有投影的概念，只是层级和坐标，可以用于直接查看任何坐标系，后面重点用于大数据配图演示


        String mbglContent = readLines("mbglbase.html");
        StringBuilder mbBuilder = new StringBuilder();

//        重新修订mapbox gl中的中心点

//        进行投影转换
        double cellSize = (indexbounds[2] - indexbounds[0])/tileSize;
        double pixelX = (centerX-indexbounds[0])/cellSize;
        double pixelY= (indexbounds[3] - centerY)/cellSize;

//        该像素点在3857坐标系下的坐标值
        double mapboxCellSize = (MAPBOX_INDEXBOUNDS[2] - MAPBOX_INDEXBOUNDS[0])/tileSize;
        double mbX=MAPBOX_INDEXBOUNDS[0] + pixelX*mapboxCellSize;
        double mbY=MAPBOX_INDEXBOUNDS[3] - pixelY*mapboxCellSize;



        mbBuilder.append(String.format("   var zoom=%d;\n",minZoom) +
                String.format("    var maxZ=%d;\n",maxZoom) +
                String.format("    var sourceName=\"%s\";\n",cacheName) +
                String.format("    var center= ol.proj.toLonLat([%f, %f]);\n",mbX,mbY));

        String jsLines=mbBuilder.toString();

        mbglContent = mbglContent.replace(REPLACEKEY,jsLines);
        String mbglFile = folder+"/indexmbgl.html";
        if(!new File(mbglFile).exists()){
            OutputStreamWriter writer  = new OutputStreamWriter(new FileOutputStream(mbglFile),"UTF-8");
            writer.write(mbglContent);
            writer.close();
        }

        if(epsgcode == 3857){
//            生成compare文件
            String compareContent = readLines("comparebase.html");
            String comparejs = "    var center = ["+centerX+", " +centerY+"];\n" +
                    "    var zoom = "+zoom+";\n" +
                    "    var minZoom ="+ minZoom+";\n" +
                    "    var maxZoom = "+maxZoom+";\n" +
                    "    var sourceName = '"+cacheName+"';";
            compareContent = compareContent.replace(REPLACEKEY,comparejs);
            String compareFile = folder+ "/compare.html";
            if(!new File(compareFile).exists()){
                OutputStreamWriter writer  = new OutputStreamWriter(new FileOutputStream(compareFile),"UTF-8");
                writer.write(compareContent);
                writer.close();
            }
        }
    }

}
