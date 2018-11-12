package iwuang;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import sun.nio.ch.IOUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;
import java.util.List;

import static org.geotools.data.Transaction.AUTO_COMMIT;

/**
 * @auther kang
 * @date 2018/8/5 19:18
 * @email iwuang@qq.com
 */
public class PostgisUtility {
    private static Logger logger = Logger.getLogger(PostgisUtility.class);
    private static PostgisDataStore postgisDataStore = null;
    private static JSONArray jsonArray = null;

    public static PostgisDataStore getPostgisDataStore() {
        return postgisDataStore;
    }

    public static void setPostgisDataStore(PostgisDataStore postgisDataStore) {

        PostgisUtility.postgisDataStore = postgisDataStore;
    }

    /***********************************************重构*************************************************************************

     /**
     * @param input      Reader InputStream File String
     * @param basevector
     * @return
     * @throws IOException
     */
    public static Vector importGeojson(Object input, Vector basevector) throws IOException {
        Vector vector = basevector;
        FeatureJSON featureJSON = new FeatureJSON();

        //转化到featureCollection
        SimpleFeatureType simpleFeatureType = featureJSON.readFeatureCollectionSchema(input, false);

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(simpleFeatureType);
        typeBuilder.add("visible", String.class);
        typeBuilder.add("id", String.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        featureJSON.setFeatureType(featureType);
        FeatureCollection featureCollection = featureJSON.readFeatureCollection(input);

        FeatureIterator iterator = featureCollection.features();
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            String visible = findVisible(feature.getIdentifier().getID());
            ((SimpleFeatureImpl) feature).setAttribute("id", feature.getIdentifier().getID());
            ((SimpleFeatureImpl) feature).setAttribute("visible", visible);
        }
        iterator.close();
        //写入postgis
        if (!write2postgis(featureCollection, vector)) return null;

        //抽取字段信息,vectorField可定制更改另用
      /*  VectorField vectorField = new VectorField(vector.getVectorid(), vector.getVectorTableName());
        vectorField = getVectorField((SimpleFeatureType) featureCollection.getSchema(), vectorField);
        vector.setVectorField(vectorField);*/
        return vector;
    }

    /**
     * geojson转成fastjson对象
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean initFeature(final File file) throws IOException {
        JSONObject jsonObject = JSONObject.parseObject(IOUtils.toString(file.toURI(), Charset.defaultCharset()));
        jsonArray = (JSONArray) jsonObject.get("features");
        if (jsonArray != null && jsonArray.size() > 0) {
            return true;
        }
        return false;
    }

    private static String findVisible(String id) {
        if (jsonArray == null || "".equals(id)) return "false";
        JSONObject jsonObject = new JSONObject();
        for (int i = 0, j = jsonArray.size(); i < j; i++) {
            jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.getString("id").equals(id)) {
                return jsonObject.getString("visible");
            }
        }
        return "false";
    }


    public static Vector importShp(String shppath, Vector basevector) throws IOException {
        if (!validateShp(shppath, true)) return null;
        Vector vector = basevector;
        ShapefileDataStore shapefileDataStore = null;
        shapefileDataStore = new ShapefileDataStore(new File(shppath).toURI().toURL());
//        shp默认使用编码ISO-8859-1,更改判断如果不是utf-8编码默认采用GBK编码
        if (!shapefileDataStore.getCharset().contains(Charset.forName("utf-8")))
            shapefileDataStore.setCharset(Charset.forName("GBK"));
        FeatureSource featureSource = shapefileDataStore.getFeatureSource();
        FeatureCollection featureCollection = featureSource.getFeatures();
        //写入postgis
        if (!write2postgis(featureCollection, vector)) return null;
        //抽取字段信息,vectorField可定制更改另用
        VectorField vectorField = new VectorField(vector.getVectorid(), vector.getVectorTableName());
        vectorField = getVectorField((SimpleFeatureType) featureCollection.getSchema(), vectorField);
        vector.setVectorField(vectorField);
        return vector;
    }

    public static void getFieldsOfShp(String shppath) throws IOException {
        if (!validateShp(shppath, true)) ;
        ShapefileDataStore shapefileDataStore = null;
        shapefileDataStore = new ShapefileDataStore(new File(shppath).toURI().toURL());
//        shp默认使用编码ISO-8859-1,更改判断如果不是utf-8编码默认采用GBK编码
        if (!shapefileDataStore.getCharset().contains(Charset.forName("utf-8")))
            shapefileDataStore.setCharset(Charset.forName("GBK"));
        FeatureSource featureSource = shapefileDataStore.getFeatureSource();
        FeatureType schema = featureSource.getSchema();
        List<String> fields = new ArrayList<>();
        Collection<PropertyDescriptor> descriptors = schema.getDescriptors();
        Iterator<PropertyDescriptor> iterator = descriptors.iterator();
        while (iterator.hasNext()) {
            PropertyDescriptor next = iterator.next();
            fields.add(next.getName().toString());
            System.out.println(next.getName().toString());
        }
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

//        shapefileDataStore.setCharset(Charset.forName("utf-8"));
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
        Map<String, Object> fields = new HashMap<>();
        Collection<PropertyDescriptor> collection = shpfeaturetype.getDescriptors();
        Iterator<PropertyDescriptor> propertyDescriptoriterator = collection.iterator();
        while (propertyDescriptoriterator.hasNext()) {
            PropertyDescriptor descriptor = propertyDescriptoriterator.next();
            fields.put(descriptor.getName().toString(), descriptor.getType().getBinding().getSimpleName());
        }

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

    //测试，更新表结构
    public static boolean temp(String tablename, String geojsonPath) throws IOException, FactoryException {
        DataStore pgDatastore = postgisDataStore.getInstance();
        FeatureSource featureSource = pgDatastore.getFeatureSource(tablename);
        FeatureType schema = featureSource.getSchema();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init((SimpleFeatureType) schema);
        typeBuilder.add(geojsonPath, String.class);
        SimpleFeatureType simpleFeatureType = typeBuilder.buildFeatureType();

        pgDatastore.updateSchema(tablename, simpleFeatureType);

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

    /****************************************工具型验证********************************************************************************
     /**************************************工具型**********************************************************************************

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

    private static boolean valiStringEmpty(String string) {
        return "".equals(string.trim()) || string == null;
    }

    /*****************************************工具型验证*******************************************************************************
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
     * 获取矢量数据字段类型相关
     *
     * @param featureType
     * @param baseVectorField 初始定义有vectorid和vectortablename
     * @return
     */
    public static VectorField getVectorField(SimpleFeatureType featureType, VectorField baseVectorField) {

        if (featureType == null || valiStringEmpty(baseVectorField.getVectorid()) || valiStringEmpty(baseVectorField.getVectorTableName()))
            return null;

        VectorField vectorField = baseVectorField;

        Map<String, Object> fields = new HashMap<>();
        Collection<PropertyDescriptor> collection = featureType.getDescriptors();
        Iterator<PropertyDescriptor> propertyDescriptoriterator = collection.iterator();
        while (propertyDescriptoriterator.hasNext()) {
            PropertyDescriptor descriptor = propertyDescriptoriterator.next();
            fields.put(descriptor.getName().toString(), descriptor.getType().getBinding().getSimpleName());
        }
        vectorField.setProperties(fields);
        return vectorField;
    }


    /**
     * 写入postgis
     *
     * @param featureCollection
     * @param vector
     * @return
     * @throws IOException
     */
    public static boolean write2postgis(FeatureCollection featureCollection, Vector vector) throws IOException {
        if (valiStringEmpty(vector.getVectorid()) || valiStringEmpty(vector.getVectorTableName())) return false;
        DataStore pgDatastore = postgisDataStore.getInstance();
        /*SimpleFeatureSource featureSource = pgDatastore.getFeatureSource(vector.getVectorTableName());
        if (featureSource != null) {
            pgDatastore.removeSchema(vector.getVectorTableName());
        }*/
        SimpleFeatureType simpleFeatureType = (SimpleFeatureType) featureCollection.getSchema();

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(simpleFeatureType);

        typeBuilder.setName(vector.getVectorTableName());
        SimpleFeatureType newtype = typeBuilder.buildFeatureType();
        pgDatastore.createSchema(newtype);

        FeatureIterator iterator = featureCollection.features();
        FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = pgDatastore.getFeatureWriterAppend(vector.getVectorTableName(), Transaction.AUTO_COMMIT);

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

    public static void clearSW() {
        Connection connection = null;
        Statement statement = null;
        try {
            String url = "jdbc:postgresql://127.0.0.1:5432/txzgy";
            String user = "postgres";
            String password = "postgres";
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("正在连接 postgis数据库" + connection);

            //relkind char r = 普通表，i = 索 引， S = 序列，v = 视 图， m = 物化视图， c = 组合类型，t = TOAST表， f = 外部 表
            String strtables = " select string_agg(relname ,\',\') from pg_class where relname like \'%sw_%\'  and relkind=\'r\' ";
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(strtables);

            while (resultSet.next()) {
                String swTablename = resultSet.getString(1);
                System.out.println(swTablename);
                ResultSet resultSet2 = statement.executeQuery(" drop table " + swTablename);
                System.out.println(resultSet2.getFetchSize());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
    }


/***********************************************重构*************************************************************************



 public void startwms(String shppath, String sldpath) throws IOException {
 ShapefileDataStore shapefileDataStore;
 shapefileDataStore = new ShapefileDataStore(new File(shppath).toURI().toURL());
 SimpleFeatureSource featureSource = shapefileDataStore.getFeatureSource("typename");
 Style style = SLD.createSimpleStyle(featureSource.getSchema());
 if (sldpath != "") {
 File sldfile = new File(sldpath);
 StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
 SLDParser sldParser = new SLDParser(styleFactory, sldfile.toURI().toURL());
 Style[] styles = sldParser.readXML();
 style = styles[0];
 } else {
 SLD.setPolyColour(style, Color.red);
 }
 // 图层
 Layer layer = new FeatureLayer(featureSource, style);

 }

 public void showweb(double[] bbox) throws FactoryException, IOException {
 double x1 = bbox[0], y1 = bbox[1],
 x2 = bbox[2], y2 = bbox[3];
 int width = 1024, height = 768;

 //        设置输出范围
 CoordinateReferenceSystem coordinateReferenceSystem = CRS.decode("EPSG:3857");
 ReferencedEnvelope maparea = new ReferencedEnvelope(x1, x2, y1, y2, coordinateReferenceSystem);
 //        初始化渲染器
 StreamingRenderer streamingRenderer = new StreamingRenderer();
 //        初始化输出图像
 BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
 Graphics graphics = bufferedImage.getGraphics();
 ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
 Rectangle rectangle = new Rectangle(0, 0, width, height);
 streamingRenderer.paint((Graphics2D) graphics, rectangle, maparea);
 ByteArrayOutputStream out = new ByteArrayOutputStream();
 boolean flag = ImageIO.write(bufferedImage, "png", out);
 /* byte[] wmsByte = out.toByteArray();
 OutputStream outputStream=response.getOutputStream();
 InputStream inputStream = new ByteArrayInputStream(wmsByte);*/


}