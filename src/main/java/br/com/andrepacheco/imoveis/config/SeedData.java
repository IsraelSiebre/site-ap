package br.com.andrepacheco.imoveis.config;

import br.com.andrepacheco.imoveis.domain.Corretor;
import br.com.andrepacheco.imoveis.domain.Imovel;
import br.com.andrepacheco.imoveis.domain.Institucional;
import br.com.andrepacheco.imoveis.repository.InstitucionalRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeedData implements ApplicationRunner {
    private final InstitucionalRepository empresa;
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper;

    public SeedData(InstitucionalRepository empresa, JdbcTemplate jdbc, JsonMapper mapper) {
        this.empresa = empresa;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // A empresa funciona como marcador: apagar imóveis/corretores não restaura a demonstração.
        if (empresa.existsById(1L)) return;
        JsonNode data;
        try (var stream = new ClassPathResource("data/seed.json").getInputStream()) {
            data = mapper.readTree(stream);
        }
        for (JsonNode node : data.get("corretores")) {
            Corretor corretor = mapper.treeToValue(node, Corretor.class);
            jdbc.update("insert into corretor (id, nome, creci, whatsapp, foto) values (?, ?, ?, ?, ?)",
                    corretor.getId(), corretor.getNome(), corretor.getCreci(), corretor.getWhatsapp(), corretor.getFoto());
        }
        for (JsonNode node : data.get("imoveis")) {
            Imovel imovel = mapper.treeToValue(node, Imovel.class);
            var local = imovel.getLocalizacao();
            jdbc.update("""
                    insert into imovel (id, aquisicao, tipo, valor, cidade, bairro, endereco,
                        corretor_id, quartos, banheiros, vagas, area_m2, descricao)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, imovel.getId(), imovel.getAquisicao(), imovel.getTipo(), imovel.getValor(),
                    local.getCidade(), local.getBairro(), local.getEndereco(),
                    imovel.getCorretor() == null ? null : imovel.getCorretor().getId(),
                    imovel.getQuartos(), imovel.getBanheiros(), imovel.getVagas(), imovel.getAreaM2(), imovel.getDescricao());
            for (int index = 0; index < imovel.getFotos().size(); index++) {
                jdbc.update("insert into imovel_fotos (imovel_id, ordem, url) values (?, ?, ?)",
                        imovel.getId(), index, imovel.getFotos().get(index));
            }
        }
        Institucional institucional = mapper.treeToValue(data.get("inst"), Institucional.class);
        institucional.setId(1L);
        empresa.saveAndFlush(institucional);
    }
}
