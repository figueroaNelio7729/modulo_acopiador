package pe.edu.upeu.sitraoro.acopio.compra.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pe.edu.upeu.sitraoro.acopio.compra.entity.TransaccionG2;

public interface TransaccionG2Repository extends JpaRepository<TransaccionG2, Long> {

    /** Partidas activas que todavia no se enviaron al mayorista ni se liquidaron. */
    @Query("""
            SELECT t FROM TransaccionG2 t JOIN FETCH t.minero
            WHERE t.idCentroAcopio = :idCentro AND t.anulada = 0
              AND t.idRecepcionMayorista IS NULL AND t.idLiquidacionG1 IS NULL
            ORDER BY t.fechaTransaccion DESC, t.idTransaccionG2 DESC
            """)
    List<TransaccionG2> disponibles(@Param("idCentro") Long idCentro);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransaccionG2 t WHERE t.idTransaccionG2 IN :ids ORDER BY t.idTransaccionG2")
    List<TransaccionG2> bloquearPorIds(@Param("ids") Collection<Long> ids);

    List<TransaccionG2> findByIdRecepcionMayoristaIn(Collection<Long> idsRecepcion);
}
