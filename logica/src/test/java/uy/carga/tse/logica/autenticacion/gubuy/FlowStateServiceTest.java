package uy.carga.tse.logica.autenticacion.gubuy;

import static org.junit.jupiter.api.Assertions.*;
import static uy.carga.tse.logica.autenticacion.gubuy.TestSupport.*;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;

class FlowStateServiceTest {

    private final FlowStateService service = new FlowStateService(
        config(false),
        CLOCK
    );
    private final FlowStateService.Flow flow = new FlowStateService.Flow(
        "st",
        "no",
        "verif",
        "https://app.carga.uy/inicio",
        null
    );

    @Test
    void firmaYValidaIdaYVuelta() {
        var leido = service.verify(service.sign(flow));
        assertEquals(flow, leido);
    }

    @Test
    void sinCodeVerifierQuedaNull() {
        var sinPkce = new FlowStateService.Flow(
            "st",
            "no",
            null,
            "https://app.carga.uy/",
            "CHOFER"
        );
        assertNull(service.verify(service.sign(sinPkce)).codeVerifier());
    }

    @Test
    void rechazaTokenAdulterado() {
        String token = service.sign(flow);
        String adulterado =
            token.substring(0, token.length() - 3) +
            (token.endsWith("AAA") ? "BBB" : "AAA");
        var e = assertThrows(GubUyAuthException.class, () ->
            service.verify(adulterado)
        );
        assertEquals(CodigoError.GUBUY_ESTADO_INVALIDO, e.codigo());
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        byte[] otra = new byte[32];
        java.util.Arrays.fill(otra, (byte) 9);
        var cfg = config(false);
        var otraConfig = new GubUyConfig(
            cfg.issuer(),
            cfg.clientId(),
            cfg.clientSecret(),
            cfg.redirectUri(),
            cfg.scopes(),
            cfg.allowedReturnPrefixes(),
            otra,
            false,
            cfg.documentClaim(),
            cfg.levelClaim(),
            cfg.connectTimeout(),
            cfg.requestTimeout(),
            3,
            cfg.breakerOpenDuration(),
            cfg.flowTtl(),
            true
        );
        String deOtro = new FlowStateService(otraConfig, CLOCK).sign(flow);
        assertThrows(GubUyAuthException.class, () -> service.verify(deOtro));
    }

    @Test
    void rechazaTokenExpirado() {
        String token = service.sign(flow);
        var seisMinutosDespues = Clock.fixed(
            NOW.plusSeconds(6 * 60),
            ZoneOffset.UTC
        );
        var tarde = new FlowStateService(config(false), seisMinutosDespues);
        assertThrows(GubUyAuthException.class, () -> tarde.verify(token));
    }

    @Test
    void rechazaVacioONull() {
        assertThrows(GubUyAuthException.class, () -> service.verify(null));
        assertThrows(GubUyAuthException.class, () -> service.verify(""));
    }
}
