package br.com.andrepacheco.imoveis.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public class Localizacao {
    @NotBlank(message = "Informe a cidade.") @Size(max = 150)
    private String cidade = "";

    @Size(max = 150)
    private String bairro = "";

    @Size(max = 300)
    private String endereco = "";

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

}

