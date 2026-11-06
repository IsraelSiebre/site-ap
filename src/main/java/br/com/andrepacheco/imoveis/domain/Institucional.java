package br.com.andrepacheco.imoveis.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "institucional")
public class Institucional {
    @Id
    private Long id = 1L;

    @NotBlank(message = "Informe o nome da empresa.") @Size(max = 150)
    private String nome = "";

    @Size(max = 30)
    private String cnpj = "";

    @Size(max = 150)
    private String instagram = "";

    @Pattern(regexp = "[0-9]{0,15}", message = "WhatsApp deve conter apenas números, com DDI e DDD.")
    private String whatsapp = "";

    @Size(max = 300)
    private String endereco = "";

    @Size(max = 20000) @Column(length = 20000)
    private String missao = "";

    @Size(max = 20000) @Column(length = 20000)
    private String visao = "";

    @Size(max = 20000) @Column(length = 20000)
    private String valoresTexto = "";

    @Size(max = 600) @Column(length = 600)
    private String bannerFundo = "";

    @NotNull @Min(0) @Max(85)
    private Integer bannerEscuro = 55;

    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "institucional_valores", joinColumns = @JoinColumn(name = "institucional_id")) @OrderColumn(name = "ordem") @Column(name = "valor", length = 150) @Size(max = 100)
    private List<@Size(max = 150, message = "Cada valor deve ter no máximo 150 caracteres.") String> valores = new ArrayList<>();

    @Valid @NotNull @Embedded
    private TerrenoConstrucao terrenoConstrucao = new TerrenoConstrucao();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getMissao() { return missao; }
    public void setMissao(String missao) { this.missao = missao; }

    public String getVisao() { return visao; }
    public void setVisao(String visao) { this.visao = visao; }

    public String getValoresTexto() { return valoresTexto; }
    public void setValoresTexto(String valoresTexto) { this.valoresTexto = valoresTexto; }

    public String getBannerFundo() { return bannerFundo; }
    public void setBannerFundo(String bannerFundo) { this.bannerFundo = bannerFundo; }

    public Integer getBannerEscuro() { return bannerEscuro; }
    public void setBannerEscuro(Integer bannerEscuro) { this.bannerEscuro = bannerEscuro; }

    public List<String> getValores() { return valores; }
    public void setValores(List<String> valores) { this.valores = valores; }

    public TerrenoConstrucao getTerrenoConstrucao() { return terrenoConstrucao; }
    public void setTerrenoConstrucao(TerrenoConstrucao terrenoConstrucao) { this.terrenoConstrucao = terrenoConstrucao; }

}

