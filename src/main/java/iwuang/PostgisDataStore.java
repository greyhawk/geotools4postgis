package iwuang;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @auther kang
 * @date 2018/8/5 19:17
 * @email iwuang@qq.com
 */
public class PostgisDataStore {
    private static Logger logger = Logger.getLogger(PostgisDataStore.class);
    private static DataStore postgisDataStore = null;
    private static String dbtype = null;
    private static String host = null;
    private static String port = null;
    private static String database = null;
    private static String schema = null;
    private static String username = null;
    private static String password = null;

    public PostgisDataStore() {
    }


    public static DataStore getInstance() {
        if (postgisDataStore == null) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(PostgisNGDataStoreFactory.DBTYPE.key, dbtype);
            params.put(PostgisNGDataStoreFactory.HOST.key, host);
            params.put(PostgisNGDataStoreFactory.PORT.key, new Integer(port));
            params.put(PostgisNGDataStoreFactory.DATABASE.key, database);
            params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
            params.put(PostgisNGDataStoreFactory.USER.key, username);
            params.put(PostgisNGDataStoreFactory.PASSWD.key, password);
            try {
                postgisDataStore = DataStoreFinder.getDataStore(params);
                logger.info("\nPostgisDataStore 初始化geotools中的 Datastore成功\n");
            } catch (IOException e) {
                logger.error("\nPostgisDataStore 初始化geotools中的 Datastore失败\n");
                logger.error(e.getMessage());
            }
        }
        return postgisDataStore;
    }

    public static String getDbtype() {
        return dbtype;
    }

    public static void setDbtype(String dbtype) {
        PostgisDataStore.dbtype = dbtype;
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        PostgisDataStore.host = host;
    }

    public static String getPort() {
        return port;
    }

    public static void setPort(String port) {
        PostgisDataStore.port = port;
    }

    public static String getDatabase() {
        return database;
    }

    public static void setDatabase(String database) {
        PostgisDataStore.database = database;
    }

    public static String getSchema() {
        return schema;
    }

    public static void setSchema(String schema) {
        PostgisDataStore.schema = schema;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        PostgisDataStore.username = username;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        PostgisDataStore.password = password;
    }
}
