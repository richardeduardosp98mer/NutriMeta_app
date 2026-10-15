package com.nutrimeta.backend.service;

import java.math.BigDecimal;

public final class ParametrosNutricionales {

    private ParametrosNutricionales() {}

    // Parametro 01 -- acordado
    public static final BigDecimal KCAL_POR_KG = new BigDecimal("7700");

    //Parametros del 02 al 06
    public static final BigDecimal DEFICIT_MAXIMO_KACL_DIA = new BigDecimal("500");
    public static final BigDecimal SUPERAVIT_MAXIMO_KACL_DIA = new BigDecimal("500");
    public static final BigDecimal PESO_KCAL_HOMBRE = new BigDecimal("1500");
    public static final BigDecimal PESO_KCAL_MUJER = new BigDecimal("1200");
    public static final BigDecimal PROTEINA_G_POR_KG= new BigDecimal("1.8");
    public static final BigDecimal GRASA_PORCENTAJE_META = new BigDecimal("0.25");

    // Parametro 07 --> propuesto
    public static final BigDecimal CANTIDAD_MAXIMA_INGREDIENTE_G = new BigDecimal("2000");
}
