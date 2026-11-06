package br.com.andrepacheco.imoveis.controller;

import br.com.andrepacheco.imoveis.domain.*;
import br.com.andrepacheco.imoveis.service.ImobiliariaService;
import br.com.andrepacheco.imoveis.service.UploadService;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Formulários HTML com sessão e CSRF: nenhuma alteração depende de uma API JavaScript. */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ImobiliariaService service;
    private final UploadService uploads;

    public AdminController(ImobiliariaService service, UploadService uploads) {
        this.service = service;
        this.uploads = uploads;
    }

    @InitBinder
    void normalizarTexto(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
        for (String campo : List.of("whatsapp", "terrenoConstrucao.whatsapp")) {
            binder.registerCustomEditor(String.class, campo, new PropertyEditorSupport() {
                @Override public void setAsText(String text) {
                    setValue(text == null ? "" : text.replaceAll("\\D", ""));
                }
            });
        }
    }

    @InitBinder("imovel")
    void bindImovel(WebDataBinder binder) {
        binder.setAllowedFields("aquisicao", "tipo", "valor", "localizacao.cidade", "localizacao.bairro",
                "localizacao.endereco", "quartos", "banheiros", "vagas", "areaM2", "descricao");
        binder.registerCustomEditor(BigDecimal.class, "valor", new PropertyEditorSupport() {
            @Override public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? BigDecimal.ZERO : new BigDecimal(text.trim()));
            }
        });
    }

    @InitBinder("corretor")
    void bindCorretor(WebDataBinder binder) {
        binder.setAllowedFields("nome", "creci", "whatsapp");
    }

    @InitBinder("empresa")
    void bindEmpresa(WebDataBinder binder) {
        binder.setAllowedFields("nome", "cnpj", "instagram", "whatsapp", "endereco", "missao", "visao",
                "valoresTexto", "bannerEscuro", "terrenoConstrucao.texto", "terrenoConstrucao.whatsapp");
    }

    @GetMapping({"", "/", "/index.html"})
    String index() { return "redirect:/admin/imoveis"; }

    @GetMapping("/login")
    String login(Model model) {
        model.addAttribute("empresaNome", service.getEmpresa().getNome());
        return "admin/login";
    }

    @GetMapping("/imoveis")
    String imoveis(Model model) { return imoveisView(model, null, null); }

    @GetMapping("/imoveis/novo")
    String novoImovel(Model model) { return imoveisView(model, new Imovel(), null); }

    @GetMapping("/imoveis/{id}/editar")
    String editarImovel(@PathVariable Long id, Model model) {
        Imovel imovel = service.buscarImovel(id);
        model.addAttribute("corretorId", imovel.getCorretor() == null ? null : imovel.getCorretor().getId());
        return imoveisView(model, imovel, id);
    }

    private String imoveisView(Model model, Imovel imovel, Long id) {
        model.addAttribute("tab", "im");
        model.addAttribute("imoveis", service.listarImoveis());
        model.addAttribute("corretores", service.listarCorretores());
        model.addAttribute("editId", id);
        if (imovel != null) model.addAttribute("imovel", imovel);
        return "admin/imoveis";
    }

    @PostMapping({"/imoveis", "/imoveis/{id}"})
    String salvarImovel(@PathVariable(required = false) Long id,
            @Valid @ModelAttribute("imovel") Imovel imovel, BindingResult errors,
            @RequestParam(required = false) Long corretorId,
            @RequestParam(defaultValue = "") List<String> ordemFotos,
            @RequestParam(required = false) List<MultipartFile> novasFotos,
            Model model, RedirectAttributes redirect) {
        List<String> anteriores = id == null ? List.of() : service.buscarImovel(id).getFotos();
        imovel.setId(id);
        imovel.setFotos(ordemFotos.stream().filter(s -> s.startsWith("e:")).map(s -> s.substring(2))
                .filter(anteriores::contains).distinct().toList());
        model.addAttribute("corretorId", corretorId);
        long novasQuantidade = novasFotos == null ? 0 : novasFotos.stream().filter(file -> !file.isEmpty()).count();
        if (imovel.getFotos().size() + novasQuantidade > 60) errors.reject("fotos", "Envie no máximo 60 fotos por imóvel.");
        if (errors.hasErrors()) return imoveisView(model, imovel, id);
        List<String> retidas = new ArrayList<>(imovel.getFotos());
        List<String> novas = new ArrayList<>();
        boolean salvo = false;
        try {
            imovel.setCorretor(corretorId == null ? null : service.buscarCorretor(corretorId));
            if (novasFotos != null) for (MultipartFile file : novasFotos) {
                if (!file.isEmpty()) novas.add(uploads.salvar(file));
            }
            List<String> fotos = new ArrayList<>();
            for (String item : ordemFotos) {
                if (item.startsWith("e:") && anteriores.contains(item.substring(2))) fotos.add(item.substring(2));
                else if (item.matches("n:[0-9]+")) {
                    int pos = Integer.parseInt(item.substring(2));
                    if (pos < novas.size()) fotos.add(novas.get(pos));
                }
            }
            // Sem JavaScript, os arquivos selecionados são acrescentados após as fotos existentes.
            novas.stream().filter(f -> !fotos.contains(f)).forEach(fotos::add);
            imovel.setFotos(new ArrayList<>(new LinkedHashSet<>(fotos)));
            service.salvarImovel(imovel);
            salvo = true;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errors.reject("save", ex.getMessage());
            return imoveisView(model, imovel, id);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND) throw ex;
            errors.reject("corretor", "O corretor selecionado não está mais disponível. Selecione outro.");
            return imoveisView(model, imovel, id);
        } catch (DataAccessException ex) {
            errors.reject("save", "Não foi possível salvar o imóvel. Tente novamente.");
            return imoveisView(model, imovel, id);
        } finally {
            if (!salvo) {
                limparUploads(novas);
                imovel.setFotos(retidas);
            }
        }
        redirect.addFlashAttribute("mensagem", "Imóvel salvo.");
        return "redirect:/admin/imoveis";
    }

    @PostMapping("/imoveis/{id}/excluir")
    String excluirImovel(@PathVariable Long id, RedirectAttributes redirect) {
        service.excluirImovel(id);
        redirect.addFlashAttribute("mensagem", "Excluído.");
        return "redirect:/admin/imoveis";
    }

    @GetMapping("/corretores")
    String corretores(Model model) { return corretoresView(model, null, null); }

    @GetMapping("/corretores/novo")
    String novoCorretor(Model model) { return corretoresView(model, new Corretor(), null); }

    @GetMapping("/corretores/{id}/editar")
    String editarCorretor(@PathVariable Long id, Model model) {
        return corretoresView(model, service.buscarCorretor(id), id);
    }

    private String corretoresView(Model model, Corretor corretor, Long id) {
        model.addAttribute("tab", "co");
        model.addAttribute("corretores", service.listarCorretores());
        model.addAttribute("editId", id);
        if (corretor != null) model.addAttribute("corretor", corretor);
        return "admin/corretores";
    }

    @PostMapping({"/corretores", "/corretores/{id}"})
    String salvarCorretor(@PathVariable(required = false) Long id,
            @Valid @ModelAttribute("corretor") Corretor corretor, BindingResult errors,
            @RequestParam(required = false) MultipartFile novaFoto,
            @RequestParam(defaultValue = "false") boolean removerFoto,
            Model model, RedirectAttributes redirect) {
        String foto = id == null ? "" : service.buscarCorretor(id).getFoto();
        corretor.setId(id);
        corretor.setFoto(removerFoto ? "" : foto);
        if (errors.hasErrors()) return corretoresView(model, corretor, id);
        List<String> novas = new ArrayList<>();
        boolean salvo = false;
        try {
            if (novaFoto != null && !novaFoto.isEmpty()) {
                String enviada = uploads.salvar(novaFoto);
                novas.add(enviada);
                corretor.setFoto(enviada);
            }
            service.salvarCorretor(corretor);
            salvo = true;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errors.reject("save", ex.getMessage());
            return corretoresView(model, corretor, id);
        } catch (DataAccessException ex) {
            errors.reject("save", "Não foi possível salvar o corretor. Tente novamente.");
            return corretoresView(model, corretor, id);
        } finally {
            if (!salvo) {
                limparUploads(novas);
                corretor.setFoto(removerFoto ? "" : foto);
            }
        }
        redirect.addFlashAttribute("mensagem", "Corretor salvo.");
        return "redirect:/admin/corretores";
    }

    @PostMapping("/corretores/{id}/excluir")
    String excluirCorretor(@PathVariable Long id, RedirectAttributes redirect) {
        service.excluirCorretor(id);
        redirect.addFlashAttribute("mensagem", "Excluído.");
        return "redirect:/admin/corretores";
    }

    @GetMapping({"/empresa", "/institucional"})
    String empresa(Model model) {
        Institucional empresa = service.getEmpresa();
        model.addAttribute("empresa", empresa);
        model.addAttribute("listaValores", String.join("\n", empresa.getValores()));
        model.addAttribute("tab", "em");
        return "admin/empresa";
    }

    @PostMapping("/empresa")
    String salvarEmpresa(@Valid @ModelAttribute("empresa") Institucional empresa, BindingResult errors,
            @RequestParam(defaultValue = "") String listaValores,
            @RequestParam(required = false) MultipartFile novoBanner,
            @RequestParam(defaultValue = "false") boolean removerBanner,
            Model model, RedirectAttributes redirect) {
        Institucional anterior = service.getEmpresa();
        empresa.setId(anterior.getId());
        empresa.setBannerFundo(removerBanner ? "" : anterior.getBannerFundo());
        empresa.setValores(new ArrayList<>(listaValores.lines().map(String::trim).filter(s -> !s.isEmpty()).toList()));
        model.addAttribute("listaValores", listaValores);
        model.addAttribute("tab", "em");
        if (errors.hasErrors()) return "admin/empresa";
        List<String> novas = new ArrayList<>();
        boolean salvo = false;
        try {
            if (novoBanner != null && !novoBanner.isEmpty()) {
                String enviada = uploads.salvar(novoBanner);
                novas.add(enviada);
                empresa.setBannerFundo(enviada);
            }
            service.salvarEmpresa(empresa);
            salvo = true;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errors.reject("save", ex.getMessage());
            return "admin/empresa";
        } catch (DataAccessException ex) {
            errors.reject("save", "Não foi possível salvar os dados da empresa. Tente novamente.");
            return "admin/empresa";
        } finally {
            if (!salvo) {
                limparUploads(novas);
                empresa.setBannerFundo(removerBanner ? "" : anterior.getBannerFundo());
            }
        }
        redirect.addFlashAttribute("mensagem", "Dados salvos.");
        return "redirect:/admin/empresa";
    }

    private void limparUploads(List<String> urls) {
        for (String url : urls) {
            try {
                uploads.removerRecente(url);
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(AdminController.class)
                        .warn("Não foi possível limpar uma foto de um cadastro não salvo: {}", url, ex);
            }
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    String arquivoGrande(RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", "Arquivo muito grande. Envie fotos de até 10 MB e no máximo 100 MB por envio.");
        return "redirect:/admin/imoveis";
    }
}
