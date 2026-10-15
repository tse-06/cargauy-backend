package uy.carga.tse.entidades.enums;

public enum EstadoGuia {
    // Le falta el vehiculo y/o el chofer
    INCOMPLETA,
    // Tiene vehiculo y chofer, todavia no se registro el inicio del viaje
    PENDIENTE,
    // Se cumplio la fecha y no se registro el inicio del viaje
    NO_REALIZADA,
    // Se registro el inicio del viaje, todavia no el fin
    EN_CURSO,
    // Se registro el fin del viaje
    FINALIZADA,
}
