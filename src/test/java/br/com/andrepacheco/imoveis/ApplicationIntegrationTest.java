package br.com.andrepacheco.imoveis;

import br.com.andrepacheco.imoveis.domain.*;
import br.com.andrepacheco.imoveis.service.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload-dir=./target/test-uploads",
        "app.admin.username=admin", "app.admin.password=integration-password"
})
@AutoConfigureMockMvc
@Transactional
class ApplicationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ImobiliariaService service;
    @Autowired UploadService uploads;

    @Test void publicPagesAlreadyContainDatabaseContentWithoutJavascript() throws Exception {
        for (String route : new String[]{"/", "/imoveis", "/sobre",
                "/contato", "/corretores", "/terreno", "/imovel?id=101"}) {
            mvc.perform(get(route)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("André Pacheco")))
                    .andExpect(content().string(not(containsString("js/mock.js"))))
                    .andExpect(content().string(not(containsString("Carregando..."))));
        }
        mvc.perform(get("/imovel").param("id", "101"))
                .andExpect(content().string(containsString("630.000,00")))
                .andExpect(content().string(containsString("/img/imoveis/101-01.jpg")))
                .andExpect(content().string(containsString("Daniel Andrade")))
                .andExpect(content().string(not(containsString(">Imprimir</button>"))))
                .andExpect(content().string(containsString("Compartilhar no WhatsApp")));
    }

    @Test void publicFiltersRunOnServerAndUnknownPropertyIs404() throws Exception {
        mvc.perform(get("/imoveis").param("cidade", "Capivari").param("valorMin", "1"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("1 imóvel encontrado")));
        mvc.perform(get("/imoveis").param("ref", "101"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("2 imóveis encontrados")));
        mvc.perform(get("/imoveis").param("aquisicao", "aluguel"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Nenhum imóvel")));
        mvc.perform(get("/imovel").param("id", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void propertyReferencesAreNotShownInPublicPagesOrAdmin() throws Exception {
        for (String route : new String[]{"/", "/imoveis", "/imovel?id=101",
                "/admin/imoveis", "/admin/imoveis/101/editar"}) {
            mvc.perform(get(route)).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Ref.:"))))
                    .andExpect(content().string(not(containsString("Referência"))))
                    .andExpect(content().string(not(containsString("<th>Ref.</th>"))))
                    .andExpect(content().string(not(containsString("Editar imóvel 101"))));
        }
    }

    @Test void authenticationAndCsrfProtectWrites() throws Exception {
        mvc.perform(get("/admin/imoveis")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post("/admin/corretores").param("nome", "Intruso"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/login").with(csrf()).param("usuario", "admin")
                        .param("senha", "wrong-password"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/admin/login?erro"));
        mvc.perform(post("/admin/login").with(csrf()).param("usuario", "admin")
                        .param("senha", "integration-password"))
                .andExpect(authenticated()).andExpect(redirectedUrl("/admin/imoveis"));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void everyAdministrativeTemplateRenders() throws Exception {
        for (String route : new String[]{"/admin/login", "/admin/imoveis", "/admin/imoveis/novo",
                "/admin/imoveis/101/editar", "/admin/corretores", "/admin/corretores/novo",
                "/admin/corretores/1/editar", "/admin/empresa"}) {
            mvc.perform(get(route)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("_csrf")));
        }
    }

    @Test @WithMockUser(roles = "ADMIN")
    void brokerCrudAndPhotoUploadArePersisted() throws Exception {
        mvc.perform(multipart("/admin/corretores").file(image("novaFoto"))
                        .param("nome", "Corretora de teste").param("creci", "123-F")
                        .param("whatsapp", "5519999999999").with(csrf()))
                .andExpect(redirectedUrl("/admin/corretores"));
        Corretor corretor = service.listarCorretores().stream()
                .filter(c -> c.getNome().equals("Corretora de teste")).findFirst().orElseThrow();
        assertThat(corretor.getFoto()).startsWith("/uploads/");
        mvc.perform(get(corretor.getFoto())).andExpect(status().isOk());
        mvc.perform(post("/admin/corretores/" + corretor.getId()).with(csrf())
                        .param("nome", "Nome atualizado").param("creci", "123-F")
                        .param("whatsapp", "5519999999999").param("removerFoto", "true"))
                .andExpect(redirectedUrl("/admin/corretores"));
        assertThat(service.buscarCorretor(corretor.getId()).getNome()).isEqualTo("Nome atualizado");
        assertThat(service.buscarCorretor(corretor.getId()).getFoto()).isEmpty();
        mvc.perform(post("/admin/corretores/" + corretor.getId() + "/excluir").with(csrf()))
                .andExpect(redirectedUrl("/admin/corretores"));
        assertThat(service.listarCorretores()).noneMatch(c -> c.getId().equals(corretor.getId()));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void propertyCrudSupportsUploadAndSelectedCover() throws Exception {
        mvc.perform(multipart("/admin/imoveis").file(image("novasFotos")).with(csrf())
                        .param("aquisicao", "aluguel").param("tipo", "apartamento")
                        .param("valor", "2500.50").param("localizacao.cidade", "Piracicaba")
                        .param("localizacao.bairro", "Centro").param("corretorId", "1")
                        .param("descricao", "Cadastro de integração").param("quartos", "2"))
                .andExpect(redirectedUrl("/admin/imoveis"));
        Imovel imovel = service.listarImoveis().stream()
                .filter(i -> i.getDescricao().equals("Cadastro de integração")).findFirst().orElseThrow();
        assertThat(imovel.getValor()).isEqualByComparingTo("2500.50");
        assertThat(imovel.getFotos()).hasSize(1);
        assertThat(imovel.getCorretor().getId()).isEqualTo(1L);
        String foto = imovel.getFotos().get(0);
        mvc.perform(post("/admin/imoveis/" + imovel.getId()).with(csrf())
                        .param("aquisicao", "compra").param("tipo", "casa").param("valor", "300000")
                        .param("localizacao.cidade", "Capivari").param("ordemFotos", "e:" + foto)
                        .param("descricao", "Atualizado"))
                .andExpect(redirectedUrl("/admin/imoveis"));
        assertThat(service.buscarImovel(imovel.getId()).getFotos()).containsExactly(foto);
        assertThat(service.buscarImovel(imovel.getId()).getDescricao()).isEqualTo("Atualizado");
        mvc.perform(post("/admin/imoveis/" + imovel.getId() + "/excluir").with(csrf()))
                .andExpect(redirectedUrl("/admin/imoveis"));
        assertThat(service.listarImoveis()).noneMatch(i -> i.getId().equals(imovel.getId()));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void propertyExistingPhotosCanBeReorderedAndRemoved() throws Exception {
        mvc.perform(post("/admin/imoveis/101").with(csrf()).param("aquisicao", "compra")
                        .param("tipo", "casa").param("valor", "630000").param("localizacao.cidade", "Capivari")
                        .param("ordemFotos", "e:/img/imoveis/101-03.jpg", "e:/img/imoveis/101-01.jpg",
                                "e:https://malicious.invalid/payload.svg"))
                .andExpect(redirectedUrl("/admin/imoveis"));
        assertThat(service.buscarImovel(101L).getFotos())
                .containsExactly("/img/imoveis/101-03.jpg", "/img/imoveis/101-01.jpg");
    }

    @Test @WithMockUser(roles = "ADMIN")
    void invalidFormsRetainDataAndDoNotPersist() throws Exception {
        long before = service.listarImoveis().size();
        mvc.perform(post("/admin/imoveis").with(csrf()).param("aquisicao", "compra")
                        .param("tipo", "casa").param("valor", "-1").param("localizacao.cidade", "Capivari"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("imovel", "valor"));
        assertThat(service.listarImoveis()).hasSize((int) before);
        mvc.perform(post("/admin/corretores").with(csrf()).param("nome", " "))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("corretor", "nome"));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void companyEditAndBannerAreVisibleInServerRenderedPages() throws Exception {
        mvc.perform(multipart("/admin/empresa").file(image("novoBanner")).with(csrf())
                        .param("nome", "Empresa de integração").param("cnpj", "62.537.875/0001-11")
                        .param("whatsapp", "5519992248572").param("endereco", "Centro")
                        .param("bannerEscuro", "65").param("missao", "Missão editada")
                        .param("visao", "Visão editada").param("listaValores", "Respeito\nQualidade")
                        .param("terrenoConstrucao.texto", "Construção editada"))
                .andExpect(redirectedUrl("/admin/empresa"));
        assertThat(service.getEmpresa().getValores()).containsExactly("Respeito", "Qualidade");
        assertThat(service.getEmpresa().getBannerFundo()).startsWith("/uploads/");
        mvc.perform(get("/sobre")).andExpect(content().string(containsString("Empresa de integração")))
                .andExpect(content().string(containsString("Missão editada")));
    }

    @Test void deletingBrokerKeepsTheirPropertiesAndClearsAssignment() {
        service.excluirCorretor(1L);
        assertThat(service.buscarImovel(101L).getCorretor()).isNull();
        assertThat(service.buscarImovel(101L).getFotos()).hasSize(26);
    }

    @Test void uploadRejectsDisguisedExecutableAndPersistsActualImage() throws Exception {
        assertThatThrownBy(() -> uploads.salvar(new MockMultipartFile("arquivo", "photo.jpg",
                "image/jpeg", "<script>alert(1)</script>".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
        String url = uploads.salvar(image("arquivo"));
        assertThat(url).startsWith("/uploads/").doesNotContain("..");
        assertThat(Files.size(Path.of("target/test-uploads", url.substring("/uploads/".length())))).isPositive();
    }

    @Test void uploadsAreConvertedToWebpWithTransparencyAndServedCorrectly() throws Exception {
        BufferedImage transparent = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
        transparent.setRGB(1, 1, 0x00ff0000);
        transparent.setRGB(2, 2, 0xffff0000);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        assertThat(ImageIO.write(transparent, "png", png)).isTrue();
        String url = uploads.salvar(new MockMultipartFile("arquivo", "foto.png", "image/png", png.toByteArray()));
        assertThat(url).matches("/uploads/[0-9a-f-]+\\.webp");
        byte[] bytes = Files.readAllBytes(Path.of("target/test-uploads", url.substring("/uploads/".length())));
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(bytes, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WEBP");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(32);
        assertThat(decoded.getHeight()).isEqualTo(24);
        assertThat((decoded.getRGB(1, 1) >>> 24) & 0xff).isZero();
        assertThat((decoded.getRGB(2, 2) >>> 24) & 0xff).isEqualTo(255);
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/webp"));

        String convertedWebp = uploads.salvar(new MockMultipartFile("arquivo", "foto.webp", "image/webp", bytes));
        assertThat(convertedWebp).endsWith(".webp");
        assertThat(ImageIO.read(Path.of("target/test-uploads", convertedWebp.substring("/uploads/".length())).toFile())).isNotNull();

        byte[] jpeg = Files.readAllBytes(Path.of("src/main/resources/static/img/imoveis/101-01.jpg"));
        String convertedJpeg = uploads.salvar(new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", jpeg));
        assertThat(convertedJpeg).endsWith(".webp");
        assertThat(ImageIO.read(Path.of("target/test-uploads", convertedJpeg.substring("/uploads/".length())).toFile())).isNotNull();
    }

    @Test void jpegOrientationIsPreservedWhenConvertingToWebp() throws Exception {
        BufferedImage source = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        assertThat(ImageIO.write(source, "jpeg", raw)).isTrue();
        byte[] jpeg = raw.toByteArray();
        byte[] exif = { (byte) 0xff, (byte) 0xe1, 0, 34, 'E', 'x', 'i', 'f', 0, 0,
                'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 18, 1, 3, 0, 1, 0, 0, 0,
                6, 0, 0, 0, 0, 0, 0, 0 };
        ByteArrayOutputStream orientedJpeg = new ByteArrayOutputStream();
        orientedJpeg.write(jpeg, 0, 2);
        orientedJpeg.write(exif);
        orientedJpeg.write(jpeg, 2, jpeg.length - 2);
        String url = uploads.salvar(new MockMultipartFile("arquivo", "celular.jpg", "image/jpeg", orientedJpeg.toByteArray()));
        BufferedImage decoded = ImageIO.read(Path.of("target/test-uploads", url.substring("/uploads/".length())).toFile());
        assertThat(decoded.getWidth()).isEqualTo(24);
        assertThat(decoded.getHeight()).isEqualTo(32);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void contentIsEscapedAndExtraFormFieldsCannotOverwriteIdentity() throws Exception {
        mvc.perform(post("/admin/corretores").with(csrf()).param("nome", "<script>alert(1)</script>")
                        .param("id", "1").param("foto", "javascript:alert(1)"))
                .andExpect(redirectedUrl("/admin/corretores"));
        assertThat(service.buscarCorretor(1L).getNome()).isEqualTo("Daniel Andrade");
        mvc.perform(get("/corretores")).andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    private MockMultipartFile image(String field) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile(field, "foto.png", "image/png", bytes.toByteArray());
    }
}
