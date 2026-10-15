package com.nutrimeta.backend.service;

import com.nutrimeta.backend.entity.Perfil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Service
public class MetaService {

    // RN-01: edad minima 18 años.
    public int calcularEdad(LocalDate fechaNacimiento) {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    // RN-03: Mifflin-St Jeor.
    public BigDecimal calcularTmb(Perfil.Sexo sexo, BigDecimal pesoKg, BigDecimal tallaCm, int edad){
        BigDecimal base = BigDecimal.TEN.multiply(pesoKg)
                .add(new BigDecimal("6.25").multiply(tallaCm))
                .subtract(new BigDecimal(5L * edad));
        return sexo == Perfil.Sexo.M ? base.add(new BigDecimal("5")) : base.subtract(new BigDecimal("161"));
    }

    // RN-04: GET = TMB x factor de actividad.
    public BigDecimal calularGet(BigDecimal tmb, BigDecimal factorActividad) {
        return tmb.multiply(factorActividad).setScale(1, RoundingMode.HALF_UP);
    }

    //Resultado del cáñculo completo.
    public record ResultadoMeta(
            BigDecimal metaKcal, BigDecimal metaProteinaG, BigDecimal metaGrasaG,
            BigDecimal metaCarbohidratosG, boolean ajustada) {}

    // RN-05 y RN-06 completos.
    public ResultadoMeta calcularMeta(
            Perfil.Sexo sexo, Perfil.Objetivo objetivo, BigDecimal pesoKg, BigDecimal pesoObjetivoKg,
            Integer plazoSemanas, BigDecimal get) {

        boolean ajustada = false;
        BigDecimal meta;

        if (objetivo == Perfil.Objetivo.MANTENER) {
            meta = get;
        } else {
            BigDecimal diasPlazo = new BigDecimal(plazoSemanas).multiply(new BigDecimal("7"));
            BigDecimal ajustePedido = pesoObjetivoKg.subtract(pesoKg).abs()
                    .multiply(ParametrosNutricionales.KCAL_POR_KG)
                    .divide(diasPlazo, 2, RoundingMode.HALF_UP);

            BigDecimal maximo = objetivo == Perfil.Objetivo.BAJAR
                    ? ParametrosNutricionales.DEFICIT_MAXIMO_KACL_DIA
                    : ParametrosNutricionales.SUPERAVIT_MAXIMO_KACL_DIA;

            BigDecimal ajusteAplicado = ajustePedido;
            if (ajustePedido.compareTo(maximo) > 0) {
                ajusteAplicado = maximo;
                ajustada = true;
            }
            meta = objetivo == Perfil.Objetivo.BAJAR ? get.subtract(ajusteAplicado) : get.add(ajusteAplicado);
        }
        BigDecimal peso = sexo == Perfil.Sexo.M
                ? ParametrosNutricionales.PESO_KCAL_HOMBRE
                : ParametrosNutricionales.PESO_KCAL_MUJER;

        if (meta.compareTo(peso) < 0) {
            meta = peso;
            ajustada = true;
        }

        BigDecimal proteinaG = ParametrosNutricionales.PROTEINA_G_POR_KG.multiply(pesoKg)
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal grasaG = meta.multiply(ParametrosNutricionales.GRASA_PORCENTAJE_META)
                .divide(new BigDecimal("9"), 1, RoundingMode.HALF_UP);
        BigDecimal carbohidratosG = meta
                .subtract(proteinaG.multiply(new BigDecimal("4")))
                .subtract(grasaG.multiply(new BigDecimal("9")))
                .divide(new BigDecimal("4"), 1, RoundingMode.HALF_UP);

        return new ResultadoMeta(meta.setScale(1, RoundingMode.HALF_UP), proteinaG, grasaG, carbohidratosG, ajustada);

    }
}
