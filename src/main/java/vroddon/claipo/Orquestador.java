package vroddon.claipo;

import vroddon.claipo.agenda.AgenteAgenda;
import vroddon.claipo.agenda.AgenteCorreo;

/**
 * Este es el orquestador. 
 * Recibe una consulta del usuario y devuelve una respuesta tras orquestar a usar.
 * @author victor
 */
public class Orquestador {

    public String orquestar(String hint) {
        String respuesta = "nada";
        String agente = analizaConsulta(hint);
        if (agente.equals("agenda")) {
            respuesta = AgenteAgenda.chat(hint);
        } else {
            respuesta = AgenteCorreo.chat(hint);
        }
        return respuesta;
    }

    private static String analizaConsulta(String orden) {
        if (orden.contains("@agenda")) {
            return "agenda";
        }
        return "correo";
    }
}
