import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BSON;
import org.bson.Document;
import org.bson.types.Binary;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class MongodbHandler extends AbstractHandler {

    private static MongoClient g_client;
    private static MongoDatabase g_db;
    private static MongoCollection<Document> g_tileCol;
    private static MongoCollection<Document> g_fontCol;
    private static MongoCollection<Document> g_imageCol;
    private static HashMap<String, byte[]> g_styles;
    private static HashMap<Integer,Double> g_levelResolutions;
    private static  String g_versionID;


    /**
     * 初始化MongoDB的访问内容
     * @param serverwithPort 数据库的IP和端口
     * @param dataBaseName 数据库的名称，对应sci文件中 MongoSetting/Database
     * @param cacheName 对应缓存的名称，对应sci文件中 MongoSetting/Name 中的内容
     * @param versionID 对应缓存的ID，对应sci文件中 VersionSetting 中的内容
     */
    public static void Init(String serverwithPort, String dataBaseName, String cacheName, String versionID,HashMap<Double,String> sclaeCatpions){
        String[] subs = serverwithPort.split(":");
        int port = 27017;
        if(subs.length > 1){
            port = Integer.valueOf(subs[1]);
        }
        g_versionID = versionID;
        g_client = new MongoClient(serverwithPort,port);
        g_db = g_client.getDatabase(dataBaseName);
        g_tileCol = g_db.getCollection("tiles_"+cacheName);
        g_imageCol=g_db.getCollection("images_"+cacheName);
        g_fontCol=g_db.getCollection("fonts");

//        直接把风格取出来,key为请求的文件名
        g_styles = new HashMap<String, byte[]>();
        MongoCollection<Document> styleCol = g_db.getCollection("styles_"+cacheName);
        BasicDBObject query = new BasicDBObject("version", g_versionID);
        FindIterable<Document> styleDocs = styleCol.find(query);
        for(Document doc : styleDocs){
            Binary bits = doc.get("style",Binary.class);
            g_styles.put("style.json",bits.getData());
            bits = doc.get("sprite_image",Binary.class);
            g_styles.put("sprite.png",bits.getData());
            bits = doc.get("sprite_json",Binary.class);
            g_styles.put("sprite.json",bits.getData());
            bits = doc.get("sprite_json_x2",Binary.class);
            g_styles.put("sprite@2x.json",bits.getData());
            bits = doc.get("sprite_image_x2",Binary.class);
            g_styles.put("sprite@2x.png",bits.getData());
            break;
        }

//        初始化分辨率与层级对照表,sclaeCatpions为比例尺与层级名称的对应关系,直接用层级做排序吧
        int levelSize = sclaeCatpions.values().size();
        ArrayList<Integer> leves = new ArrayList<Integer>(levelSize);
        for (String s : sclaeCatpions.values()){
            leves.add(Integer.valueOf(s));
        }
        Collections.sort(leves);

        g_levelResolutions = new HashMap<Integer,Double>();
        MongoCollection<Document> metadataCol = g_db.getCollection("metadatas");
        BasicDBObject meataquery= new BasicDBObject("tilesetName",cacheName);
        FindIterable<Document> docs = metadataCol.find(meataquery);
        for(Document doc: docs){
            ArrayList<Double> ress = doc.get("resolutions",ArrayList.class);

            for(int i =0; i< ress.size(); i++){
                long nl = (long) ((ress.get(i) + 0.0000005)*1000000);
                double nresult = nl/1000000.0;
                g_levelResolutions.put(leves.get(i),nresult);
            }
            break;
        }
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//        从MongoDB中读取出对应的数据, 根据文件的目录判断数据的类型，然后用不同的方式来处理
        byte[] result = null;
        if(target.endsWith(".mvt")){
//            处理瓦片
            String[] lrc = cfFileHandler.urlToLRC(target);
            int level = Integer.valueOf(lrc[0]);

            HashMap<String,Object> queryMap = new HashMap<String,Object>();
            if(g_levelResolutions.containsKey(level)){
                queryMap.put("resolution",Double.valueOf(g_levelResolutions.get(level).toString()));
                queryMap.put("tile_row",Integer.valueOf(lrc[1]));
                queryMap.put("tile_column",Integer.valueOf(lrc[2]));
                queryMap.put("name",g_versionID);
                BasicDBObject query = new BasicDBObject(queryMap);
                FindIterable<Document> docs = g_tileCol.find(query);

                for(Document doc : docs){
//                获取tileID后再到image表中查询
                    String tileID = doc.get("tile_id", String.class);
                    BasicDBObject tile_query = new BasicDBObject("tile_id", tileID);
                    FindIterable<Document> tiles = g_imageCol.find(tile_query);
                    for (Document t:tiles){
                        result = t.get("tile_data",Binary.class).getData();
                        break;
                    }
                    break;
                }
            }
        } else if(target.endsWith(".pbf")){
//            处理字体
            String[] subs = target.split("/");
            String fileName = subs[subs.length - 1];
            String fontName = subs[subs.length - 2];
            HashMap<String,Object> queryMap = new HashMap<String,Object>();
            queryMap.put("fontName",fontName);
            queryMap.put("fileName",fileName);
            BasicDBObject query = new BasicDBObject(queryMap);

            FindIterable<Document> docs = g_fontCol.find(query);
            for(Document doc : docs){
                result = doc.get("fontData", Binary.class).getData();
                break;
            }
        } else if(target.endsWith(".png")){
            String key = new File(target).getName();
            result = g_styles.get(key);
        } else if(target.endsWith(".json")){
            String key = new File(target).getName();
            result = g_styles.get(key);
            response.setContentType("application/json;charset=utf-8");
        }

        if(result != null){
            response.getOutputStream().write(result);
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
        }
    }
}
