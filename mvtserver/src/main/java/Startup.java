import com.supermap.data.Point2D;
import com.supermap.data.ProjectionType;
import com.supermap.data.Rectangle2D;
import com.supermap.data.Unit;
import com.supermap.data.processing.CacheWriter;
import com.supermap.data.processing.StorageType;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;



public class Startup {
    /**
     * 根据端口，sci文件的设置
     * @param args port, sci file
     * @throws Exception
     */
    public  static void main(String[] args) throws Exception {
        long start=System.currentTimeMillis();
        int port = Integer.valueOf(args[0]);
        String sciPath=args[1];
//        第三个参数为postgis的连接信息
        String pgConnector = "Host=192.168.13.192;Port=5430;Username=postgres;Password=supermap;Database=china";
        if(args.length > 2){

        }
        Server server = new Server(port);

        File sciFile = new File(sciPath);
        if(!sciFile.exists()){
            System.out.println(sciFile+" does not exist.");
        }
//        按照梯次提交原始文件、紧凑文件、MongoDB、默认处理这些处理链条
        HandlerList handlers = new HandlerList();
        CacheWriter sciInfos=  new CacheWriter();
        sciInfos.FromConfigFile(sciPath);

//        TODO: 需要根据后面自行读取坐标系，不过对于不严格的服务来看，使用经纬度和米两种坐标基本可以覆盖所有应用
        int epsgcode = sciInfos.getPrjCoordSys().getEPSGCode();
        if(epsgcode == 0){
            if(sciInfos.getPrjCoordSys().getCoordUnit() == Unit.DEGREE){
                epsgcode = 4326;
            }else if(sciInfos.getPrjCoordSys().getProjection().getType() == ProjectionType.PRJ_SPHERE_MERCATOR ||
                    sciInfos.getPrjCoordSys().getProjection().getType() == ProjectionType.PRJ_MERCATOR) {
                epsgcode = 3857;
            }
        }

        Point2D center = sciInfos.getCacheBounds().getCenter();
        HashMap<Double,String> sclaeCatpions = sciInfos.getCacheScaleCaptions();
        int levelSize = sclaeCatpions.values().size();
        ArrayList<Double> scales = new ArrayList<Double>(levelSize);
        scales.addAll(sclaeCatpions.keySet());
        Collections.sort(scales);


        int minZoom = Integer.valueOf(sclaeCatpions.get(scales.get(0)));
        double maxZoomScale = scales.get(levelSize-1);
        int maxZoom =Integer.valueOf(sclaeCatpions.get(maxZoomScale));
//        缩小可以在最小的基础上，缩小2级，放到可以在最大的基础上放到2级
//        minZoom -=2;
//        if(minZoom < 0){
//            minZoom =0;
//        }
//        maxZoom+=2;
        double[] ress=new double[maxZoom+1];
        ress[maxZoom] = sciInfos.getReolustion(maxZoomScale);
        for(int i=ress.length-2; i> -1; i--){
            ress[i] = ress[i+1]*2;
        }
        Rectangle2D indexBounds = sciInfos.getIndexBounds();
        String sciFolder = sciFile.getParent();

        double[] indexExtent = new double[]{indexBounds.getLeft(),indexBounds.getBottom(),indexBounds.getRight(),indexBounds.getTop()};
        // 如果indexbounds小于mapbounds的时候中心点使用indexBounds的中心点
        if(sciInfos.getCacheBounds().contains(indexBounds)){
            center = indexBounds.getCenter();
        }
        HtmlGenerator.generateOL4(sciFolder,sciInfos.getCacheName(),epsgcode,minZoom,maxZoom,ress,center.x,center.y,indexExtent,512);

        //        用于处理原始文件
        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setResourceBase(sciFolder);


        if(sciInfos.getStorageType() == StorageType.MongoDB){
            String[] mongoInfos = sciInfos.getMongoConnectionInfo();
            MongodbHandler.Init(mongoInfos[0],mongoInfos[1],mongoInfos[2],sciInfos.getVersionSetting(),sclaeCatpions);
            handlers.setHandlers(new Handler[] { new MongodbHandler(),resource_handler,new DefaultHandler() });
        }else if(sciInfos.getStorageType() == StorageType.Compact){
            handlers.setHandlers(new Handler[] { new cfFileHandler(sciFolder),resource_handler,new DefaultHandler() });
        }else {
            handlers.setHandlers(new Handler[] { new EanbleCORSHandler(),resource_handler,new DefaultHandler() });
        }
        server.setHandler(handlers);

        URI host = server.getURI();
        System.out.println("open by openlayers viewer http://"+host.getHost()+":"+port+"/index.html");

        if(epsgcode == 3857) {
            System.out.println("open by mapbox-gl viewer  http://" + host.getHost()+":"+port + "/indexmbgl.html");
        }

        server.start();
        server.join();

    }

//    private static void enableCORS(final String origin, final String methods, final String headers) {
//        Server server = new Server(9091);
//        options("*", (request, response) -> {
//            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
//            if (accessControlRequestHeaders != null) {
//                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
//            }
//
//            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
//
//            if (accessControlRequestMethod != null) {
//                response.header("Access-Control-Allow-Methods",
//                        accessControlRequestMethod);
//            }
//
//            return "OK";
//        });
//
//        before((request, response) -> {
//            response.header("Access-Control-Allow-Origin", origin);
//            response.header("Access-Control-Request-Method", methods);
//            response.header("Access-Control-Allow-Headers", headers);
//        });
//    }
}
