package ssii2.voto.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.naming.InitialContext;

/**
 *
 * @author jaime
 */
public class DBTester {

    // Información de conexión
    // Para conexiones directas, requerimos: driver, cadena de conexión,
    // usuario y clave
    private static final String JDBC_DRIVER =
           "org.postgresql.Driver";

    // TODO: Definir la cadena de conexion a la base de datos
    /*********************************************************/
    private static final String JDBC_CONNSTRING =
            "jdbc:postgresql://10.2.8.2:5432/voto";
    /*********************************************************/
    private static final String JDBC_USER = "alumnodb";
    private static final String JDBC_PASSWORD = "alumnodb";

    // Para conexión por datasource, sólo necesitamos su nombre
    // TODO: Definir el nombre del datasource
    /*************************************************/
    private static final String JDBC_DSN =
            "jdbc/VotoDB";
    /*************************************************/

    // Modo inicial de conexión (directo|dsn)
    private boolean directConnection = false;

    // Datasource para conexiones por pool
    private DataSource ds = null;

    // Información de debug
    private int     dsConnectionCount = 0;
    private int     directConnectionCount = 0;

    
    
    public DBTester() {

        try {

            // Para conexiones directas, instanciamos el driver
            Class.forName(JDBC_DRIVER).getDeclaredConstructor().newInstance();

            // Para conexiones con pool, preparamos un datasource
            // Buscar el datasource por JNDI
            ds = (DataSource) new InitialContext().lookup(JDBC_DSN);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Obtiene una nueva conexión del pool
    protected synchronized Connection getConnection()
            throws SQLException {

        if (!directConnection) {
            dsConnectionCount++;
            // Obtener una conexión del pool
            return ds.getConnection();
        } else {
            directConnectionCount++;
            // Obtener una nueva conexión
            return DriverManager.getConnection(JDBC_CONNSTRING,
                                               JDBC_USER, JDBC_PASSWORD);
        }
    }

    // Cerrar o devolver la conexión al pool
    protected synchronized void closeConnection(Connection c)
            throws SQLException {
        c.close();
        if (!directConnection) {
            dsConnectionCount--;
        } else {
            directConnectionCount--;
        }
    }

    /*
     * Métodos de ayuda para depuración
     */


    /**
     * @return the directConnectionCount
     */
    public int getDirectConnectionCount() {
        return directConnectionCount;
    }

    /**
     * @return the dsnConnectionCount
     */
    public int getDSNConnectionCount() {
        return dsConnectionCount;
    }

    /**
     * @return the pooled
     */
    public boolean isDirectConnection() {
        return directConnection;
    }

    /**
     * @param directConnection valor de conexión directa o indirecta
     */
    public void setDirectConnection(boolean directConnection) {
        this.directConnection = directConnection;
    }


}
