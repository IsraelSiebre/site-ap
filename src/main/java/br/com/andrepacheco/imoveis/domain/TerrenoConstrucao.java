package br.com.andrepacheco.imoveis.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public class TerrenoConstrucao {
    @Size(max = 20000) @Column(length = 20000)
    private String texto = "";

    @Pattern(regexp = "[0-9]{0,15}", message = "WhatsApp deve conter apenas números, com DDI e DDD.") @Column(name = "terreno_whatsapp")
    private String whatsapp = "";

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

}

