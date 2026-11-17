package pe.edu.upeu.sitraoro.acopio.minero.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MINEROS")
@Getter
@Setter
@NoArgsConstructor
public class Minero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MINERO")
    private Long idMinero;

    @Column(name = "DOCUMENTO_IDENTIDAD", nullable = false, unique = true, length = 15)
    private String documentoIdentidad;

    @Column(name = "NOMBRES_APELLIDOS", nullable = false, length = 150)
    private String nombresApellidos;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "ZONA_PROCEDENCIA", length = 100)
    private String zonaProcedencia;

    @Column(name = "ID_CENTRO_ACOPIO")
    private Long idCentroAcopio;

    @Column(name = "FECHA_REGISTRO", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
