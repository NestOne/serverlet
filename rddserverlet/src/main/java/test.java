import com.supermap.bdt.geotools.SGeometry;
import com.supermap.bdt.io.SGeometryReadWriter;
import com.supermap.data.Point2D;

/**
 * Created by Administrator on 2017/10/30.
 */
public class test {
    //
    public static void main(String[] args) throws Exception {
        SGeometry geoPnt = SGeometry.fromPoint2D(new Point2D(10, 20));
        String json = SGeometryReadWriter.toGeoJson(geoPnt);
        System.out.println(json);

        SGeometry newOne = SGeometryReadWriter.fromGeoJson(json);
        System.out.println(SGeometryReadWriter.toGeoJson(newOne));
    }
}
