package pe.edu.upeu.sitraoro;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sin login no hay cuenta de acopiador: el centro de acopio sobre el que trabaja este sistema se fija
 * en application.yml (sitra.acopio.id-centro). Por defecto: centro 1 "Acopio Maria".
 */
@Component
public class CentroActual {

    private final Long id;

    public CentroActual(@Value("${sitra.acopio.id-centro:1}") Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
