/**
 * Pr&aacute;ctricas de Sistemas Inform&aacute;ticos II
 * VisaCancelacionJMSBean.java
 */

package ssii2.voto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jakarta.ejb.EJBException;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.jms.MessageListener;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.jms.JMSException;
import jakarta.annotation.Resource;
import java.util.logging.Logger;

/**
 * @author jaime y daniel
 */
/**
 * This class represents a Message-Driven Bean (MDB) that handles cancellation votes.
 * It listens to a specific JMS queue and cancels votes based on the received messages.
 */
@MessageDriven(mappedName = "jms/VotosQueue")
public class VotoCancelacionJMSBean extends DBTester implements MessageListener {
  static final Logger logger = Logger.getLogger("VotoCancelacionJMSBean");
  @Resource
  private MessageDrivenContext mdc;

  private static final String SELECT_CODIGO_QRY = 
            "select codRespuesta from voto"
            + " where idVoto=?";

  private static final String UPDATE_CANCELA_QRY =
                "update voto set codRespuesta ='999'" +
                " where idVoto=?";

  private static final String UPDATE_NUMERO_VOTOS_QRY = 
                "update censo"
                + " set numerovotosrestantes=numerovotosrestantes+1" 
                + " where numerodni=?";

  private static final String SELECT_NUMERO_DNI = 
                "select numerodni"
                + " from voto"
                + " where idVoto=?";
  /**
   * Default constructor for VotoCancelacionJMSBean.
   */
  public VotoCancelacionJMSBean() {
  }

  /**
   * This method is called when a message is received by the MDB.
   * It cancels a vote based on the received message.
   *
   * @param inMessage the received message
   */
  public void onMessage(Message inMessage) {
      TextMessage msg = null;
      String idVoto = null;
      Connection con = null;
      ResultSet rs = null, rsDNI = null;
      PreparedStatement pstmt = null, pstmt2 = null, pstmt3 = null, pstmt4 = null;

      try {
          if (inMessage instanceof TextMessage) {
            msg = (TextMessage) inMessage;
            logger.info("MESSAGE BEAN: Message received: " + msg.getText());
            
            con = getConnection();
            idVoto = msg.getText();

            String select = SELECT_CODIGO_QRY;
            logger.info(select);
            pstmt = con.prepareStatement(select);
            pstmt.setInt(1, Integer.parseInt(idVoto));
            rs = pstmt.executeQuery();

            if (rs.next()){
                  String codRespuesta = rs.getString("codRespuesta");
                  if (codRespuesta.equals("000")){
                      String update = UPDATE_CANCELA_QRY;
                      logger.info(update);
                      pstmt2 = con.prepareStatement(update);
                      pstmt2.setInt(1, Integer.parseInt(idVoto));
                      logger.info("Voto cancelado");
                      if (pstmt2.executeUpdate() == 1) {
                        String select2 = SELECT_NUMERO_DNI;
                        logger.info(select);
                        pstmt4 = con.prepareStatement(select2);
                        pstmt4.setInt(1, Integer.parseInt(idVoto));
                        rsDNI = pstmt4.executeQuery();
                        if (rsDNI.next()) {
                            logger.info("DNI obtenido");
                            String update2 = UPDATE_NUMERO_VOTOS_QRY;
                            logger.info(update2);
                            pstmt3 = con.prepareStatement(update2);
                            pstmt3.setString(1, rsDNI.getString("numerodni"));
                            
                            if (pstmt3.executeUpdate() == 1) {
                                logger.info("Numero de votos actualizado");
                            } else {
                                logger.info("Error en la actualizacion de numerovotosrestante");
                            }
                        
                        } else {
                            logger.info("Error en la obtencion del voto");
                        }                      

                      } else {
                        logger.info("Error en la actualizacion de codRespuesta");
                      }
                      
                  } else {
                      logger.info("Voto no cancelado");
                  }
              } else {
                  logger.info("Voto no encontrado");
            }

            
        } else {
            logger.warning(
                    "Message of wrong type: "
                    + inMessage.getClass().getName());
        }
    } catch (JMSException e) {
        e.printStackTrace();
        mdc.setRollbackOnly();
    } catch (Throwable te) {
        te.printStackTrace();
    }
    finally {
        try {
            if (rs != null) {
                rs.close(); rs = null;
            }
            if (pstmt != null) {
                pstmt.close(); pstmt = null;
            }
            if (pstmt2 != null) {
                pstmt2.close(); pstmt2 = null;
            }
            if (pstmt3 != null) {
                pstmt3.close(); pstmt3 = null;
            }
            if (pstmt4 != null) {
                pstmt4.close(); pstmt4 = null;
            }
            if (con != null) {
                closeConnection(con); con = null;
            } 
        } catch (SQLException e) {
        }
      }
  }
}
