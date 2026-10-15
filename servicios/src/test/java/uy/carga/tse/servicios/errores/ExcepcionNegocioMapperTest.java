package uy.carga.tse.servicios.errores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.autenticacion.CredencialesInvalidasException;
import uy.carga.tse.logica.autenticacion.RolNoAsignadoException;
import uy.carga.tse.logica.autenticacion.RolRequeridoException;
import uy.carga.tse.logica.autenticacion.TokenInvalidoException;
import uy.carga.tse.logica.autenticacion.gubuy.GubUyAuthException;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;
import uy.carga.tse.logica.comun.excepciones.ConflictoException;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

/**
 * Solo el estado: armar la Response necesita la implementacion de JAX-RS,
 * que la da el servidor.
 */
class ExcepcionNegocioMapperTest {

    @Test
    void cadaCategoriaTieneSuEstado() {
        CodigoError c = CodigoError.DATOS_INVALIDOS;
        assertEquals(
            Status.BAD_REQUEST,
            ExcepcionNegocioMapper.estadoDe(
                new ValidacionNegocioException(c, "x")
            )
        );
        assertEquals(
            Status.FORBIDDEN,
            ExcepcionNegocioMapper.estadoDe(new AccesoDenegadoException(c, "x"))
        );
        assertEquals(
            Status.NOT_FOUND,
            ExcepcionNegocioMapper.estadoDe(
                new RecursoNoEncontradoException(c, "x")
            )
        );
        assertEquals(
            Status.CONFLICT,
            ExcepcionNegocioMapper.estadoDe(new ConflictoException(c, "x"))
        );
        assertEquals(
            Status.SERVICE_UNAVAILABLE,
            ExcepcionNegocioMapper.estadoDe(
                new ServicioNoDisponibleException(c, "x")
            )
        );
    }

    @Test
    void lasSubclasesHeredanElEstadoDeSuCategoria() {
        assertEquals(
            Status.UNAUTHORIZED,
            ExcepcionNegocioMapper.estadoDe(
                new CredencialesInvalidasException()
            )
        );
        assertEquals(
            Status.UNAUTHORIZED,
            ExcepcionNegocioMapper.estadoDe(new TokenInvalidoException("x"))
        );
        assertEquals(
            Status.UNAUTHORIZED,
            ExcepcionNegocioMapper.estadoDe(
                new GubUyAuthException(CodigoError.GUBUY_TOKEN_INVALIDO, "x")
            )
        );
        assertEquals(
            Status.BAD_REQUEST,
            ExcepcionNegocioMapper.estadoDe(
                new RolRequeridoException(List.of(Rol.CHOFER, Rol.FUNCIONARIO))
            )
        );
        assertEquals(
            Status.FORBIDDEN,
            ExcepcionNegocioMapper.estadoDe(
                new RolNoAsignadoException(Rol.CHOFER)
            )
        );
    }
}
