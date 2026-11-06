package br.com.andrepacheco.imoveis;

import br.com.andrepacheco.imoveis.config.SeedData;
import br.com.andrepacheco.imoveis.domain.Corretor;
import br.com.andrepacheco.imoveis.domain.Imovel;
import br.com.andrepacheco.imoveis.service.ImobiliariaService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in: use somente com um banco PostgreSQL de teste descartável. */
@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
@SpringBootTest
@Transactional
class PostgresIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("POSTGRES_TEST_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("POSTGRES_TEST_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("POSTGRES_TEST_PASSWORD"));
        // Explicitamente restrito ao banco de teste informado por POSTGRES_TEST_URL.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.upload-dir", () -> "./target/postgres-test-uploads");
        registry.add("app.admin.password", () -> "test-only-password");
    }

    @Autowired ImobiliariaService service;
    @Autowired SeedData seed;
    @Autowired EntityManager entityManager;

    @Test void initialDataAndCrudWorkOnPostgres() throws Exception {
        assertThat(service.getEmpresa().getNome()).contains("Pacheco");
        assertThat(service.listarCorretores()).hasSize(3);
        assertThat(service.listarImoveis()).hasSize(2);
        assertThat(service.buscarImovel(101L).getFotos()).hasSize(26);
        seed.run(null);
        assertThat(service.listarImoveis()).hasSize(2);

        Corretor corretor = new Corretor();
        corretor.setNome("Corretor PostgreSQL");
        corretor = service.salvarCorretor(corretor);
        assertThat(corretor.getId()).isGreaterThanOrEqualTo(1000L);

        Imovel imovel = new Imovel();
        imovel.setTipo("casa");
        imovel.setAquisicao("compra");
        imovel.setValor(new BigDecimal("123456.78"));
        imovel.getLocalizacao().setCidade("Capivari");
        imovel.setCorretor(corretor);
        imovel.getFotos().add("/img/imoveis/101-01.jpg");
        imovel = service.salvarImovel(imovel);
        Long id = imovel.getId();
        assertThat(id).isGreaterThanOrEqualTo(1000L);
        entityManager.flush();
        entityManager.clear();
        assertThat(service.buscarImovel(id).getValor()).isEqualByComparingTo("123456.78");
        assertThat(service.buscarImovel(id).getFotos()).containsExactly("/img/imoveis/101-01.jpg");
        service.excluirCorretor(corretor.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(service.buscarImovel(id).getCorretor()).isNull();
        service.excluirImovel(id);
        entityManager.flush();
        assertThat(service.listarImoveis()).hasSize(2);
    }
}
