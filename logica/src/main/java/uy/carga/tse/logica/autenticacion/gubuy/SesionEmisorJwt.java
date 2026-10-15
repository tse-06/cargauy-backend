package uy.carga.tse.logica.autenticacion.gubuy;

import jakarta.ejb.EJBException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.autenticacion.AutenticacionServicioLocal;
import uy.carga.tse.logica.autenticacion.AutenticacionServicioLocal.Cuenta;
import uy.carga.tse.logica.autenticacion.RolesUsuario;
import uy.carga.tse.logica.autenticacion.ServicioJwt;
import uy.carga.tse.logica.autenticacion.Sesion;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;
import uy.carga.tse.logica.comun.excepciones.ConflictoException;
import uy.carga.tse.logica.usuario.UsuarioServicioLocal;
import uy.carga.tse.logica.usuario.UsuarioServicioLocal.NuevoUsuario;

/**
 * Caso de uso 3.4.7: convierte una identidad verificada por gub.uy en el JWT
 * de sesion. Busca al usuario por cedula; si es su primera autenticacion
 * obtiene sus datos de la PDI, verifica la mayoria de edad y lo registra;
 * elige el rol activo y emite el JWT con {@link ServicioJwt}.
 * <p>
 * Es un bean CDI sin transaccion a proposito: cada llamada a un EJB corre y
 * hace commit en su propia transaccion. Asi el alta de un usuario nuevo queda
 * guardada aunque el login termine en USUARIO_SIN_ROLES (los roles se los
 * asignan despues), y la consulta SOAP a la PDI no retiene una transaccion de
 * la base abierta.
 */
@ApplicationScoped
public class SesionEmisorJwt {

    private static final Logger log = LoggerFactory.getLogger(
        SesionEmisorJwt.class
    );

    static final int EDAD_MINIMA = 18;

    /**
     * Roles que pueden entrar con gub.uy. Administrador y Autoridad usan el
     * mecanismo interno y nunca deben recibir un token por esta via, aunque el
     * usuario los tenga asignados.
     */
    static final Set<Rol> ROLES_GUBUY = EnumSet.of(
        Rol.CHOFER,
        Rol.ENCARGADO_EMPRESA,
        Rol.FUNCIONARIO
    );

    private final ServicioJwt jwt;
    private final AutenticacionServicioLocal autenticacion;
    private final UsuarioServicioLocal usuarios;
    private final PersonasPdi pdi;
    private final Clock reloj;

    /** Requerido por CDI para crear el proxy del bean. */
    protected SesionEmisorJwt() {
        this.jwt = null;
        this.autenticacion = null;
        this.usuarios = null;
        this.pdi = null;
        this.reloj = null;
    }

    @Inject
    public SesionEmisorJwt(
        ServicioJwt jwt,
        AutenticacionServicioLocal autenticacion,
        UsuarioServicioLocal usuarios,
        PersonasPdi pdi
    ) {
        this(jwt, autenticacion, usuarios, pdi, Clock.systemDefaultZone());
    }

    public SesionEmisorJwt(
        ServicioJwt jwt,
        AutenticacionServicioLocal autenticacion,
        UsuarioServicioLocal usuarios,
        PersonasPdi pdi,
        Clock reloj
    ) {
        this.jwt = jwt;
        this.autenticacion = autenticacion;
        this.usuarios = usuarios;
        this.pdi = pdi;
        this.reloj = reloj;
    }

    /**
     * @param identidad     identidad ya verificada por ID Uruguay
     * @param rolSolicitado rol con el que el usuario quiere entrar; null si el
     *                      cliente no lo indico (solo es valido si el usuario
     *                      tiene un unico rol habilitado para gub.uy)
     * @return el JWT firmado por {@code ServicioJwt}
     * @throws uy.carga.tse.logica.comun.excepciones.ExcepcionNegocio si el usuario
     *         no puede entrar: menor de edad, sin rol, rol no habilitado para
     *         gub.uy o rol ambiguo
     */
    public String emitirToken(GubUyIdentity identidad, Rol rolSolicitado) {
        return emitirSesion(identidad, rolSolicitado).token();
    }

    /**
     * Igual que {@link #emitirToken} pero devuelve la sesion completa (token,
     * vencimiento y rol activo): la web necesita el vencimiento para la cookie.
     */
    public Sesion emitirSesion(GubUyIdentity identidad, Rol rolSolicitado) {
        if (rolSolicitado != null && !ROLES_GUBUY.contains(rolSolicitado)) {
            throw new AccesoDenegadoException(
                CodigoError.ROL_NO_PERMITIDO_GUBUY,
                "Ese rol no inicia sesion con gub.uy"
            );
        }

        Cuenta cuenta = autenticacion
            .cuentaPorCi(identidad.documento())
            .orElseGet(() -> registrarNuevo(identidad));

        Set<Rol> habilitados = EnumSet.noneOf(Rol.class);
        habilitados.addAll(cuenta.roles());
        habilitados.retainAll(ROLES_GUBUY);

        Rol activo = RolesUsuario.elegir(habilitados, rolSolicitado);
        return jwt.emitir(cuenta.id(), activo);
    }

    private Cuenta registrarNuevo(GubUyIdentity identidad) {
        String ci = identidad.documento();
        if (identidad.email() == null || identidad.email().isBlank()) {
            throw new AccesoDenegadoException(
                CodigoError.EMAIL_REQUERIDO,
                "gub.uy no informo un correo electronico"
            );
        }
        PersonasPdi.DatosPersona persona = pdi
            .obtener(ci)
            .orElseThrow(() ->
                new AccesoDenegadoException(
                    CodigoError.PERSONA_NO_ENCONTRADA,
                    "No se pudieron obtener los datos de la persona"
                )
            );
        int edad = Period.between(
            persona.fechaNacimiento(),
            LocalDate.now(reloj)
        ).getYears();
        if (edad < EDAD_MINIMA) {
            log.info("Primera autenticacion rechazada: menor de edad");
            throw new AccesoDenegadoException(
                CodigoError.MENOR_DE_EDAD,
                "Solo pueden ingresar personas mayores de edad"
            );
        }
        NuevoUsuario nuevo = new NuevoUsuario(
            ci,
            persona.nombre(),
            persona.apellido(),
            identidad.email(),
            persona.fechaNacimiento()
        );
        try {
            UUID id = usuarios.registrarCiudadano(nuevo);
            return new Cuenta(id, Set.of());
        } catch (ConflictoException e) {
            if (e.codigo() != CodigoError.CI_EN_USO) {
                throw e; // el correo lo usa otro usuario
            }
            // otro primer login del mismo usuario lo registro despues de la busqueda
            return autenticacion.cuentaPorCi(ci).orElseThrow(() -> e);
        } catch (EJBException e) {
            // los dos logins pasaron la verificacion a la vez y la constraint
            // unique rechazo el segundo insert (el contenedor hizo rollback)
            return autenticacion.cuentaPorCi(ci).orElseThrow(() -> e);
        }
    }
}
