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

    /**
     * 将资源中的所有css, js文件保存到folder的./dist目录下
     * @param folder
     */
    public static void saveAsJS(String folder) throws Exception {
        ClassLoader loader =HtmlGenerator.class.getClassLoader();
        String distFolder = "dist";


        String classResourceName = HtmlGenerator.class.getName().replace(".", "/") + ".class";
        URL classResourceURL = loader.getResource(classResourceName);
        String classResourcePath = classResourceURL.getPath();
        List<String> filenames = new ArrayList<>();

        if (classResourceURL.getProtocol().equals("file")) {
            // 开发环境里class和resource同位于target/classes目录下
            BufferedReader in = new BufferedReader(new InputStreamReader(loader.getResourceAsStream(distFolder)));
            String resource;
            while( (resource = in.readLine()) != null ) {
                filenames.add( distFolder+"/"+resource );
            }
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
        iniMap.append(String.format("    var minZ=%d;\n", minZoom));
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
        if(epsgcode == 3857){
            String mbglContent = readLines("mbglbase.html");
            String jsLines = "var root = window.location.href;\n" +
                    "    root = root.substring(0,root.lastIndexOf(\"/\")+1);\n" +
                    "\n" +
                    "    fetch('./styles/style.json').then(function (response) {\n" +
                    "        response.json().then(function (glStyle) {\n" +
                    "            // 修改style,sprite, glyphs为绝对路径\n" +
                    "            var target =  root + glStyle.sources[\""+cacheName+"\"].tiles[0];\n" +
                    "            glStyle.sources[\""+cacheName+"\"].tiles[0]=target;\n" +
                    "            glStyle.sprite =root +glStyle.sprite;\n" +
                    "            glStyle.glyphs = root+ glStyle.glyphs;\n" +
                    "            mapboxgl.accessToken = 'pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4M29iazA2Z2gycXA4N2pmbDZmangifQ.-g_vE53SD2WrJ6tFX7QHmA';\n" +
                    "            var map = new mapboxgl.Map({\n" +
                    "                container: 'map', // container id\n" +
                    "                style: glStyle,\n" +
                    "                center: ol.proj.toLonLat(["+centerX+", "+centerY+"]),\n" +
                    "                zoom: "+minZoom+",\n" +
                    "                minZoom:"+minZoom+",\n" +
                    "                maxZoom:"+maxZoom+"\n" +
                    "            });\n" +
                    "            // map.showCollisionBoxes = true;\n" +
                    "            // map.showTileBoundaries = true;\n" +
                    "            var nav = new mapboxgl.NavigationControl();\n" +
                    "            map.addControl(nav, 'top-left');\n" +
                    "            var sca = new mapboxgl.ScaleControl({maxWith:80,unit:'imperial'});\n" +
                    "            map.addControl(sca);\n" +
                    "        });\n" +
                    "    });\n";

            mbglContent = mbglContent.replace(REPLACEKEY,jsLines);
            String mbglFile = folder+"/indexmbgl.html";
            if(!new File(mbglFile).exists()){
                OutputStreamWriter writer  = new OutputStreamWriter(new FileOutputStream(mbglFile),"UTF-8");
                writer.write(mbglContent);
                writer.close();
            }

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
