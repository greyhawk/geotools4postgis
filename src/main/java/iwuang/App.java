package iwuang;

import org.opengis.referencing.FactoryException;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, FactoryException {PostgisDataStore postgisDataStore = new PostgisDataStore();
        postgisDataStore.setHost("127.0.0.1");
        postgisDataStore.setPort("5432");
// 扩展数据库类型，读取postgis该参数类型设置为postgis
// 其余设置对应的数据库类型，要求jdbc支持并且引入相关库
        postgisDataStore.setDbtype("postgis");
        postgisDataStore.setDatabase("test");
        postgisDataStore.setSchema("public");
        postgisDataStore.setUsername("postgres");
        postgisDataStore.setPassword("postgres");

        PostgisUtility.setPostgisDataStore(postgisDataStore);

        PostgisUtility.importGeojson("E:\\summary\\geotoos4postgis\\src\\main\\java\\yieryi\\line.geojson", "sw_line");
        PostgisUtility.importShp("E:\\\\summary\\\\geotoos4postgis\\\\src\\\\main\\\\java\\\\yieryi\\\\test\\\\test.shp", "sw_line2");
        PostgisUtility.exportShp("sw_line", "E:\\summary\\geotoos4postgis\\src\\main\\java\\yieryi\\test\\test.shp");
        PostgisUtility.exportGeojson("sw_line", "E:\\summary\\geotoos4postgis\\src\\main\\java\\yieryi\\a.geojson");

        System.out.println( "Hello World!" );
    }
}
