package br.com.andrepacheco.imoveis.service;

import br.com.andrepacheco.imoveis.domain.*;
import br.com.andrepacheco.imoveis.repository.*;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ImobiliariaService {
    private final ImovelRepository imoveis;
    private final CorretorRepository corretores;
    private final InstitucionalRepository empresa;
    private final Validator validator;

    public ImobiliariaService(ImovelRepository imoveis, CorretorRepository corretores, InstitucionalRepository empresa, Validator validator) {
        this.imoveis = imoveis;
        this.corretores = corretores;
        this.empresa = empresa;
        this.validator = validator;
    }

    public Institucional getEmpresa() {
        return empresa.findById(1L).orElseGet(Institucional::new);
    }

    public List<Corretor> listarCorretores() { return corretores.findAll(Sort.by("id")); }
    public List<Imovel> listarImoveis() { return imoveis.findAll(Sort.by("id")); }

    public Imovel buscarImovel(Long id) {
        return imoveis.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imóvel não encontrado."));
    }

    public Corretor buscarCorretor(Long id) {
        return corretores.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Corretor não encontrado."));
    }

    @Transactional
    public Imovel salvarImovel(Imovel imovel) {
        if (imovel.getId() != null) buscarImovel(imovel.getId());
        if (imovel.getCorretor() != null) imovel.setCorretor(buscarCorretor(imovel.getCorretor().getId()));
        if (imovel.getValor() == null) imovel.setValor(BigDecimal.ZERO);
        if (imovel.getLocalizacao() == null) imovel.setLocalizacao(new Localizacao());
        if (imovel.getFotos() == null) imovel.setFotos(new ArrayList<>());
        imovel.getFotos().forEach(ImobiliariaService::validarFoto);
        validar(imovel);
        return imoveis.save(imovel);
    }

    @Transactional
    public Corretor salvarCorretor(Corretor corretor) {
        if (corretor.getId() != null) buscarCorretor(corretor.getId());
        validarFoto(corretor.getFoto());
        validar(corretor);
        return corretores.save(corretor);
    }

    @Transactional
    public Institucional salvarEmpresa(Institucional institucional) {
        institucional.setId(1L);
        validarFoto(institucional.getBannerFundo());
        if (institucional.getValores() == null) institucional.setValores(new ArrayList<>());
        if (institucional.getTerrenoConstrucao() == null) institucional.setTerrenoConstrucao(new TerrenoConstrucao());
        validar(institucional);
        return empresa.save(institucional);
    }

    @Transactional
    public void excluirImovel(Long id) { imoveis.delete(buscarImovel(id)); }

    @Transactional
    public void excluirCorretor(Long id) {
        Corretor corretor = buscarCorretor(id);
        for (Imovel imovel : imoveis.findAll()) {
            if (imovel.getCorretor() != null && id.equals(imovel.getCorretor().getId())) imovel.setCorretor(null);
        }
        imoveis.flush();
        corretores.delete(corretor);
    }

    private static void validarFoto(String url) {
        if (url == null || url.isBlank()) return;
        if (!url.matches("/(?:img|uploads)/[a-zA-Z0-9_./-]+") || url.contains("..")) {
            throw new IllegalArgumentException("Selecione uma imagem enviada pelo painel.");
        }
    }

    private void validar(Object entidade) {
        var erros = validator.validate(entidade);
        if (!erros.isEmpty()) {
            String message = erros.stream().map(e -> e.getPropertyPath() + ": " + e.getMessage())
                    .sorted().collect(java.util.stream.Collectors.joining(" "));
            throw new IllegalArgumentException(message);
        }
    }
}
