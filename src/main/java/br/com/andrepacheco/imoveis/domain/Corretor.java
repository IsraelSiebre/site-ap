package br.com.andrepacheco.imoveis.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "corretor")
public class Corretor {
    @Id @SequenceGenerator(name = "corretor_seq", sequenceName = "corretor_seq", initialValue = 1000, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "corretor_seq")
    private Long id;

    @NotBlank(message = "Informe o nome.") @Size(max = 150)
    private String nome = "";

    @Size(max = 50)
    private String creci = "";

    @Pattern(regexp = "[0-9]{0,15}", message = "WhatsApp deve conter apenas números, com DDI e DDD.")
    private String whatsapp = "";

    @Size(max = 600) @Column(length = 600)
    private String foto = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCreci() { return creci; }
    public void setCreci(String creci) { this.creci = creci; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

}

