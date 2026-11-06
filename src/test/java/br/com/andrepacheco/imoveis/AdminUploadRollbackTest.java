package br.com.andrepacheco.imoveis;

import br.com.andrepacheco.imoveis.service.ImobiliariaService;
import br.com.andrepacheco.imoveis.service.UploadService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload-dir=./target/test-uploads",
        "app.admin.username=admin", "app.admin.password=integration-password"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminUploadRollbackTest {
    @Autowired MockMvc mvc;
    @Autowired UploadService uploads;
    @Autowired ImobiliariaService service;

    @Test void invalidSecondPhotoRemovesFirstUploadAndPreservesExistingProperty() throws Exception {
        long before = files();
        var fotos = java.util.List.copyOf(service.buscarImovel(101L).getFotos());
        mvc.perform(multipart("/admin/imoveis/101")
                        .file(image("novasFotos"))
                        .file(new MockMultipartFile("novasFotos", "invalid.png", "image/png", "invalid".getBytes()))
                        .with(csrf())
                        .param("aquisicao", "compra").param("tipo", "casa").param("valor", "630000")
                        .param("localizacao.cidade", "Capivari").param("ordemFotos", "e:" + fotos.get(0)))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("imovel"));
        assertThat(files()).isEqualTo(before);
        assertThat(service.buscarImovel(101L).getFotos()).containsExactlyElementsOf(fotos);
    }

    @Test void rejectedCompanyValuesRemoveUploadedBanner() throws Exception {
        long before = files();
        String oldBanner = service.getEmpresa().getBannerFundo();
        mvc.perform(multipart("/admin/empresa").file(image("novoBanner")).with(csrf())
                        .param("nome", "Empresa teste").param("bannerEscuro", "55")
                        .param("listaValores", "x".repeat(151)))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("empresa"));
        assertThat(files()).isEqualTo(before);
        assertThat(service.getEmpresa().getBannerFundo()).isEqualTo(oldBanner);
    }

    private long files() throws Exception {
        try (var stream = Files.list(uploads.getDirectory())) { return stream.count(); }
    }

    private MockMultipartFile image(String field) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile(field, "foto.png", "image/png", bytes.toByteArray());
    }
}
