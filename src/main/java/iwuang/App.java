package iwuang;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.opengis.referencing.FactoryException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException, FactoryException {
        PostgisDataStore postgisDataStore = new PostgisDataStore();
        postgisDataStore.setHost("127.0.0.1");
        postgisDataStore.setPort("5432");
// 扩展数据库类型，读取postgis该参数类型设置为postgis
// 其余设置对应的数据库类型，要求jdbc支持并且引入相关库
        postgisDataStore.setDbtype("postgis");
        postgisDataStore.setDatabase("txzgy");
        postgisDataStore.setSchema("public");
        postgisDataStore.setUsername("postgres");
        postgisDataStore.setPassword("postgres");
//        PostgisUtility.setPostgisDataStore(postgisDataStore);

//        PostgisUtility.importGeojson("D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\line.geojson", "sw_line2");
//        PostgisUtility.importShp("D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\test\\test.shp", "sw_line2");


//        PostgisUtility.exportShp("sw_line", "E:\\summary\\geotoos4postgis\\src\\main\\java\\yieryi\\test\\test.shp");
//        PostgisUtility.exportGeojson("sw_line", "E:\\summary\\geotoos4postgis\\src\\main\\java\\yieryi\\a.geojson");

//=====================================重构后===========================================================
//        File file=new File("D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\line.geojson");
        Date date = new Date();
        File file = new File("D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\d.geojson");
        Vector vector = new Vector();
        vector.setVectorid("123456790");
        vector.setVectorTableName("sw_cesium");
//        PostgisUtility.initFeature(file);
//        vector = PostgisUtility.importShp("D:\\Users\\Administrator\\Documents\\Tencent Files\\917485769\\FileRecv\\全国省级、地市级、县市级行政区划shp\\县级_截止08年_shp\\China counties\\counties_china.shp", vector);
        System.out.println(vector.toString());
//        vector = PostgisUtility.importGeojson(file, vector);

//        PostgisUtility.exportGeojson("sw_cesium", "D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\test.json");
        //更新表结构，失败
//        PostgisUtility.temp("sw_cesium", "test");
//        PostgisUtility.exportShp("sw_2a1b972244bb48ba94c56cc236d392d1", "D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\test\\ss\\aa.shp");

        PostgisUtility.getFieldsOfShp("D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\test\\bigshp\\MuchBigPolygon.shp");

//        String string = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},
//        InputStream inputStream = new ByteArrayInputStream(string.getBytes());
//        PostgisUtility.importGeojson(inputStream, vector);
//        PostgisUtility.exportShp("sw_gbk1", "D:\\Users\\Administrator\\Documents\\GitHub\\geotools4postgis\\src\\main\\java\\iwuang\\test\\test.shp");

//        PostgisUtility.importShp("D:\\Users\\Administrator\\Documents\\Tencent Files\\917485769\\FileRecv\\shpfiletest（仅用于测试）\\shpfiletest\\Export_Output_TXtest.shp", vector);
//        System.out.println( "Hello World!" );
//        PostgisUtility.clearSW();
        long sec = new Date().getTime() - date.getTime();

        System.out.println(sec);
    }
}
