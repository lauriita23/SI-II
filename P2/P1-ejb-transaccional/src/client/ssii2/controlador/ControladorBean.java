package ssii2.controlador;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import java.io.Serializable;
import jakarta.faces.context.*;
import jakarta.xml.ws.BindingProvider;

import ssii2.voto.*;
import ssii2.interaccion.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import jakarta.ejb.EJB;
import ssii2.servicio.dao.VotoDAORemote;

/*
 * Managed Bean de ambito de sesion que recoge los datos de la votacion.
 */

@Named // Permite acceder al bean a traves del EL
@SessionScoped  // Hace que el bean persista en la sessión

public class ControladorBean implements Serializable {

    // Referencia obtenida por inyección
    // a un bean de sesión con la información del voto
    
    @Inject private VotoBean voto; 

    // Referencia obtenida por inyección
    // a un bean de sesión con la información de la interacción
    // con el modelo de negocio 

    @Inject private InteraccionBean interaccion; 

	@EJB(name = "VotoDAOBean", beanInterface = VotoDAORemote.class)
 	private VotoDAORemote dao;


    public ControladorBean() {
    }

	private ssii2.servicio.VotoBean traducirVotoParaServicio(VotoBean voto) {
		ssii2.servicio.CensoBean censo_nuevo = new ssii2.servicio.CensoBean();
		ssii2.servicio.VotoBean voto_nuevo = new ssii2.servicio.VotoBean();

		voto_nuevo.setIdCircunscripcion(voto.getIdCircunscripcion());
		voto_nuevo.setIdMesaElectoral(voto.getIdMesaElectoral());
		voto_nuevo.setIdProcesoElectoral(voto.getIdProcesoElectoral());
		voto_nuevo.setNombreCandidatoVotado(voto.getNombreCandidatoVotado());

		voto_nuevo.setIdVoto(this.voto.getIdVoto());
		voto_nuevo.setMarcaTiempo(this.voto.getMarcaTiempo());
		voto_nuevo.setCodigoRespuesta(this.voto.getCodigoRespuesta());

		censo_nuevo.setNumeroDNI(voto.getCenso().getNumeroDNI());
		censo_nuevo.setFechaNacimiento(voto.getCenso().getFechaNacimiento());
		censo_nuevo.setNombre(voto.getCenso().getNombre());
		censo_nuevo.setCodigoAutorizacion(voto.getCenso().getCodigoAutorizacion());
		

		voto_nuevo.setCenso(censo_nuevo);
		return voto_nuevo;
	}

    // Metodo que recibe la acción de enviar el voto e interactua con la lógica de negocio
    // encargada de registrar el voto


    public String enviarVoto() {
		ssii2.servicio.VotoBean votoAux = null;


	    if (this.interaccion.getDebug() == true) {
	    	this.escribirLog("Solicitado el registro del voto.");
	    }

	    /* Instanciamos el objeto que presta la lógica de negocio de la aplicación */

	    dao.setDirectConnection(this.interaccion.getConexionDirecta());
	    dao.setDebug(this.interaccion.getDebug());
	    dao.setPrepared(this.interaccion.getPreparedStatements());

	    /* Comprobamos que el ciudadano está en el censo */
		//votoAux = this.traducirVotoParaServicio(this.voto);
	    if (dao.compruebaCenso(this.traducirVotoParaServicio(this.voto).getCenso()) == false) {

			String error_msg = "¡El ciudadano no se encuentra en el censo!";

			if (this.interaccion.getDebug() == true) {
				this.escribirLog(error_msg);
			}

			this.setMensajeError(error_msg);
				return "error";
	    } 
	
	    /* Registramos el voto */
		try{
			votoAux = dao.registraVoto(this.traducirVotoParaServicio(this.voto));
			if (votoAux == null){
				String error_msg = "¡No se ha podido registrar el voto!";

				if (this.interaccion.getDebug() == true) {
					this.escribirLog(error_msg);
				}

				this.setMensajeError(error_msg);
					return "error";
			}
			this.voto.setIdVoto(votoAux.getIdVoto());
			this.voto.setCodigoRespuesta(votoAux.getCodigoRespuesta());
			this.voto.setMarcaTiempo(votoAux.getMarcaTiempo());

		}catch(Exception e) {
			this.escribirLog(e.toString());
		} 
	

	    /* Todo ha ido bien. Vamos a la página de respuesta */

	    if (this.interaccion.getDebug() == true) {
	    	this.escribirLog("¡Voto registrado correctamente!");
	    }

	    return "respuesta";
    }

    // Metodo que recibe la acción de borrar los votos de un proceso electoral
    // e interactua con la lógica de negocio para llevarlo a cabo

    public String borrarVotos() {

	    /* Instanciamos el objeto que presta la lógica de negocio de la aplicación */


	    // Borramos los votos

	    int votos_borrados = dao.delVotos(traducirVotoParaServicio(this.voto).getIdProcesoElectoral());

	    // Comprobamos el nº de votos borrados

	    if (votos_borrados != 0) {
	    	FacesContext.getCurrentInstance().getExternalContext().getSessionMap().
			put("numVotosBorrados", String.valueOf(votos_borrados));

	    	return "borradook";
	    } 
	    
	    String error_msg = "¡No se ha podido borrar ningún voto!";
	    this.setMensajeError(error_msg);

	    return "error";
    }

    // Metodo que recibe la acción de consultar los votos de un proceso electoral
    // e interactua con la lógica de negocio para llevarlo a cabo

    public String consultarVotos() {

	    /* Instanciamos el objeto que presta la lógica de negocio de la aplicación */


	    // Obtenemos los votos
		
		ssii2.servicio.VotoBean v = traducirVotoParaServicio(this.voto);

		ssii2.servicio.VotoBean[] votos = dao.getVotos(v.getIdProcesoElectoral());

		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().
			put("votosObtenidos", votos);

	    return "listadoVotos";
    }

    // Método que escribe en el log del servidor

    private void escribirLog(String log) {
	    System.out.println("[LOG INFO]:" + log + "\n");
    }

    // Metodo que fija en contexto un mensaje de error para ser mostrado en una página xhtml
    
    private void setMensajeError(String mensaje) {
	    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("error", mensaje);
    }

	

}

