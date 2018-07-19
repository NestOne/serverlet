import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import com.supermap.data.processing.CompactFile;

/**
 * 用于处理cf文件的资源请求，主要是解压文件中的内容
 */
public class cfFileHandler extends AbstractHandler {
    private String m_basePath = "";

    public cfFileHandler(String basePath) {
        m_basePath = basePath;
    }

    /**
     * 将路径转成 层，行，列的字符串数组，去掉后缀
     *
     * @param url
     * @return
     */
    public static String[] urlToLRC(String url) {
        //        根据target字符串进行处理,"tiles/{z}/{x}/{y}.mvt"
        String[] subs = url.split("/");
        String levelStr = subs[subs.length - 3];
        String columnStr = subs[subs.length - 2];

        String rowStr = subs[subs.length - 1];
        rowStr = rowStr.substring(0, rowStr.length() - 4);
        String tilesFolder = subs[subs.length - 4];

        return new String[]{levelStr, rowStr, columnStr,tilesFolder};
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        if(!target.endsWith(".mvt")){
            return;
        }
        String[] lrc=urlToLRC(target);

        String levelStr = lrc[0];
        int row = Integer.valueOf(lrc[1]);
        int column = Integer.valueOf(lrc[2]);
        String tileFoder = lrc[lrc.length-1];

        String cfFileName = m_basePath + "/" + tileFoder + "/" + levelStr + "/" + row / 128 + "/" + column / 128 + ".cf";
//        System.out.println(cfFileName);
        int rowIndexIncf = row % 128;
        int columnIndexIncf = column % 128;

        if (new File(cfFileName).exists()) {
            CompactFile cf = new CompactFile();
            int openState = cf.Open(cfFileName, "");
            byte[] data = cf.getAt(rowIndexIncf, columnIndexIncf);
            cf.dispose();
            if (data != null) {
                response.getOutputStream().write(data);
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            baseRequest.setHandled(true);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }


    }
}
