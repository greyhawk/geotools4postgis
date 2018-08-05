package iwuang;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.geotools.data.Transaction.AUTO_COMMIT;

/**
 * @auther kang
 * @date 2018/8/5 19:18
 * @email iwuang@qq.com
 */
public class PostgisUtility {
    private static Logger logger = Logger.getLogger(PostgisUtility.class);
    private static PostgisDataStore postgisDataStore = null;

    public static PostgisDataStore getPostgisDataStore() {
        return postgisDataStore;
    }


    public static void setPostgisDataStore(PostgisDataStore postgisDataStore) {

        PostgisUtility.postgisDataStore = postgisDataStore;
    }

    /**
     * shp文件导入到postgis
     *
     * @param shppath   shp文件路径，包括文件.shp的拓展名
     * @param tablename 存储到postgis中的表名称
     * @return
     * @throws IOException
     */
    public static boolean importShp(String shppath, String tablename) throws IOException {

        if (!validateShp(shppath, true)) return false;
        DataStore pgDatastore = postgisDataStore.getInstance();

        ShapefileDataStore shapefileDataStore = null;
        shapefileDataStore = new ShapefileDataStore(new File(shppath).toURI().toURL());

        shapefileDataStore.setCharset(Charset.forName("utf-8"));
        FeatureSource featureSource = shapefileDataStore.getFeatureSource();
        FeatureCollection featureCollection = featureSource.getFeatures();
        SimpleFeatureType shpfeaturetype = shapefileDataStore.getSchema();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(shpfeaturetype);
        typeBuilder.setName(tablename);
        SimpleFeatureType newtype = typeBuilder.buildFeatureType();
        pgDatastore.createSchema(newtype);
        logger.info("\npostgis创建数据表成功");

        FeatureIterator iterator = featureCollection.features();
        FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = pgDatastore.getFeatureWriterAppend(tablename, Transaction.AUTO_COMMIT);

        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            SimpleFeature simpleFeature = featureWriter.next();
            Collection<Property> properties = feature.getProperties();
            Iterator<Property> propertyIterator = properties.iterator();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.next();
                simpleFeature.setAttribute(property.getName().toString(), property.getValue());
            }
            featureWriter.write();
        }
        iterator.close();
        featureWriter.close();
        shapefileDataStore.dispose();
        pgDatastore.dispose();
        logger.info("\nshp导入postgis成功");
        return true;
    }

    /**
     * geojson 文件导入的Postgis数据库
     *
     * @param geojsonpath geojson文件存储路径，包括文件拓展名.json或者.geojson
     * @param tablename   自定义存储到postgis数据库存储的表的名称
     * @return 导入结果
     * @throws IOException
     */
    public static boolean importGeojson(String geojsonpath, String tablename) throws IOException {
        if (!validateGeojson(geojsonpath, true)) return false;
        DataStore pgDatastore = postgisDataStore.getInstance();
        FeatureJSON featureJSON = new FeatureJSON();
        FeatureCollection featureCollection = featureJSON.readFeatureCollection(new FileInputStream(geojsonpath));
        SimpleFeatureType geojsontype = (SimpleFeatureType) featureCollection.getSchema();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(geojsontype);
        typeBuilder.setName(tablename);
        SimpleFeatureType newtype = typeBuilder.buildFeatureType();
        pgDatastore.createSchema(newtype);

        FeatureIterator iterator = featureCollection.features();
        FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = pgDatastore.getFeatureWriterAppend(tablename, Transaction.AUTO_COMMIT);

        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            SimpleFeature simpleFeature = featureWriter.next();
            Collection<Property> properties = feature.getProperties();
            Iterator<Property> propertyIterator = properties.iterator();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.next();
                simpleFeature.setAttribute(property.getName().toString(), property.getValue());
            }
            featureWriter.write();
        }
        iterator.close();
        featureWriter.close();
        pgDatastore.dispose();
        return true;
    }

    /**
     * postgis矢量数据表导入shp文件
     *
     * @param tablename 矢量数据表表名称
     * @param shpPath   shp文件路径，包括文件.shp的拓展名
     * @return 导出完成
     * @throws IOException
     * @throws FactoryException
     */
    public static boolean exportShp(String tablename, String shpPath) throws IOException, FactoryException {
        DataStore pgDatastore = postgisDataStore.getInstance();
        FeatureSource featureSource = pgDatastore.getFeatureSource(tablename);
        FeatureCollection featureCollection = featureSource.getFeatures();
        FeatureIterator<SimpleFeature> iterator = featureCollection.features();
        SimpleFeatureType pgfeaturetype = pgDatastore.getSchema(tablename);
        File file = new File(shpPath);

        if (!validateShp(shpPath, false)) return false;

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
        ShapefileDataStore shpDataStore = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);
        String srid = pgfeaturetype.getGeometryDescriptor().getUserData().get("nativeSRID").toString();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(pgfeaturetype);
        if (!srid.equals("0")) {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:" + srid, true);
            typeBuilder.setCRS(crs);
        }
        pgfeaturetype = typeBuilder.buildFeatureType();
        shpDataStore.setCharset(Charset.forName("utf-8"));
        shpDataStore.createSchema(pgfeaturetype);

        FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = shpDataStore.getFeatureWriter(shpDataStore.getTypeNames()[0], AUTO_COMMIT);

        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            SimpleFeature simpleFeature = featureWriter.next();
            Collection<Property> properties = feature.getProperties();
            Iterator<Property> propertyIterator = properties.iterator();

            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.next();
                if (geomfield(property.getName().toString())) {
                    simpleFeature.setAttribute("the_geom", property.getValue());
                    continue;
                }
                simpleFeature.setAttribute(property.getName().toString(), property.getValue());
            }
            featureWriter.write();
        }
        iterator.close();
        featureWriter.close();
        pgDatastore.dispose();
        return true;
    }

    /**
     * 根据用途判断shppath的有效性
     *
     * @param shppath
     * @param forread
     * @return
     */
    private static boolean validateShp(String shppath, boolean forread) {
        File file = new File(shppath);
        String ends = shppath.substring(shppath.lastIndexOf('.'), shppath.length());
        File parent = new File(shppath.substring(0, shppath.lastIndexOf('\\')));
        if (!parent.exists()) {
            parent.mkdirs();
        }
        return ".shp".equals(ends.toLowerCase()) && (forread == file.exists());
    }

    /**
     * 根据用途判断geojsonpath的有效性
     *
     * @param geojsonpath
     * @param forread
     * @return
     */
    private static boolean validateGeojson(String geojsonpath, boolean forread) {
        File file = new File(geojsonpath);
        String ends = geojsonpath.substring(geojsonpath.lastIndexOf('.'), geojsonpath.length());
        File parent = new File(geojsonpath.substring(0, geojsonpath.lastIndexOf('\\')));
        if (!parent.exists()) {
            parent.mkdirs();
        }
        return (".json".equals(ends.toLowerCase()) || ".geojson".equals(ends.toLowerCase())) && (forread == file.exists());
    }

    /**
     * postgis数据表表对应常用的geom字段名称判断
     * geotools中SimplpeFeatureType对应geometrydescriptions 的属性名对应的为"the_geom"
     *
     * @param filedname 字段名称
     * @return
     */
    private static boolean geomfield(String filedname) {
        return "the_geom".equals(filedname.toLowerCase()) || "geom".equals(filedname.toLowerCase()) || "geometry".equals(filedname.toLowerCase()) || "geo".equals(filedname.toLowerCase());
    }

    /**
     * postgis矢量数据表导出geojson
     *
     * @param tablename
     * @param geojsonPath
     * @return
     * @throws IOException
     * @throws FactoryException
     */
    public static boolean exportGeojson(String tablename, String geojsonPath) throws IOException, FactoryException {
        DataStore pgDatastore = postgisDataStore.getInstance();
        FeatureSource featureSource = pgDatastore.getFeatureSource(tablename);
        FeatureCollection featureCollection = featureSource.getFeatures();
        FeatureJSON featureJSON = new FeatureJSON();
        File file = new File(geojsonPath);
        if (!file.exists()) {
            file.createNewFile();
        }
        OutputStream outputStream = new FileOutputStream(file, false);
        featureJSON.writeFeatureCollection(featureCollection, outputStream);
        pgDatastore.dispose();
        return true;
    }

}
