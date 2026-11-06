package br.com.andrepacheco.imoveis.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "imovel")
public class Imovel {
    @Id @SequenceGenerator(name = "imovel_seq", sequenceName = "imovel_seq", initialValue = 1000, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "imovel_seq")
    private Long id;

    @NotBlank @Pattern(regexp = "compra|aluguel", message = "Escolha compra ou aluguel.")
    private String aquisicao = "compra";

    @NotBlank @Pattern(regexp = "casa|apartamento|terreno", message = "Escolha um tipo de imóvel válido.")
    private String tipo = "casa";

    @NotNull @DecimalMin(value = "0", message = "O valor não pode ser negativo.") @Digits(integer = 12, fraction = 2) @Column(precision = 14, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    @Valid @NotNull @Embedded
    private Localizacao localizacao = new Localizacao();

    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "imovel_fotos", joinColumns = @JoinColumn(name = "imovel_id")) @OrderColumn(name = "ordem") @Column(name = "url", length = 600) @Size(max = 60)
    private List<String> fotos = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "corretor_id")
    private Corretor corretor;

    @Min(value = 0, message = "Quartos não pode ser negativo.") @Max(1000)
    private Integer quartos;

    @Min(value = 0, message = "Banheiros não pode ser negativo.") @Max(1000)
    private Integer banheiros;

    @Min(value = 0, message = "Vagas não pode ser negativo.") @Max(1000)
    private Integer vagas;

    @DecimalMin(value = "0", message = "A área não pode ser negativa.") @Digits(integer = 12, fraction = 2) @Column(precision = 14, scale = 2)
    private BigDecimal areaM2;

    @Size(max = 20000) @Column(length = 20000)
    private String descricao = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAquisicao() { return aquisicao; }
    public void setAquisicao(String aquisicao) { this.aquisicao = aquisicao; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public Localizacao getLocalizacao() { return localizacao; }
    public void setLocalizacao(Localizacao localizacao) { this.localizacao = localizacao; }

    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }

    public Corretor getCorretor() { return corretor; }
    public void setCorretor(Corretor corretor) { this.corretor = corretor; }

    public Integer getQuartos() { return quartos; }
    public void setQuartos(Integer quartos) { this.quartos = quartos; }

    public Integer getBanheiros() { return banheiros; }
    public void setBanheiros(Integer banheiros) { this.banheiros = banheiros; }

    public Integer getVagas() { return vagas; }
    public void setVagas(Integer vagas) { this.vagas = vagas; }

    public BigDecimal getAreaM2() { return areaM2; }
    public void setAreaM2(BigDecimal areaM2) { this.areaM2 = areaM2; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

}

