package br.com.andrepacheco.imoveis.controller;

import br.com.andrepacheco.imoveis.domain.Imovel;
import br.com.andrepacheco.imoveis.domain.Institucional;
import br.com.andrepacheco.imoveis.service.ImobiliariaService;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class SiteController {
    private final ImobiliariaService service;
    private final ViewHelper view;
    public SiteController(ImobiliariaService service, ViewHelper view) { this.service = service; this.view = view; }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("view", view);
        Institucional company = service.getEmpresa();
        List<Imovel> catalog = service.listarImoveis();
        model.addAttribute("inst", company);
        model.addAttribute("ano", Year.now().getValue());
        model.addAttribute("catalogo", catalog);
        model.addAttribute("corretores", service.listarCorretores());
        model.addAttribute("localidades", catalog.stream().filter(x -> x.getLocalizacao() != null && x.getLocalizacao().getCidade() != null)
                .map(x -> x.getLocalizacao().getCidade() + "|" + text(x.getLocalizacao().getBairro())).distinct().toList());
        // Only the data needed by the browser's rules engine is embedded; conversations stay in JS.
        model.addAttribute("chatImoveis", catalog.stream().map(x -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", x.getId()); data.put("aquisicao", x.getAquisicao()); data.put("tipo", x.getTipo());
            data.put("valor", x.getValor()); data.put("localizacao", x.getLocalizacao());
            return data;
        }).toList());
        Map<String, Object> chatCompany = new LinkedHashMap<>();
        chatCompany.put("nome", company.getNome()); chatCompany.put("whatsapp", company.getWhatsapp());
        chatCompany.put("terrenoConstrucao", company.getTerrenoConstrucao());
        model.addAttribute("chatEmpresa", chatCompany);
    }

    @GetMapping("/")
    String home(Model model) {
        page(model, "index", "");
        model.addAttribute("destaques", catalog(model).stream().limit(6).toList());
        return "index";
    }

    @GetMapping("/imoveis")
    String listings(@RequestParam Map<String, String> parameters, Model model) {
        Map<String, String> filters = new LinkedHashMap<>();
        for (String key : List.of("aquisicao", "tipo", "valorMin", "valorMax", "cidade", "bairro", "ord"))
            filters.put(key, parameters.getOrDefault(key, "").trim());
        String loc = parameters.getOrDefault("loc", "");
        if (!loc.isBlank()) {
            String[] parts = loc.split("\\|", 2);
            filters.put("cidade", parts[0]); filters.put("bairro", parts.length == 2 ? parts[1] : "");
        }
        BigDecimal minimum = amount(filters.get("valorMin")), maximum = amount(filters.get("valorMax"));
        List<Imovel> matching = new ArrayList<>(catalog(model).stream().filter(x -> {
            String city = x.getLocalizacao() == null ? "" : text(x.getLocalizacao().getCidade());
            String neighborhood = x.getLocalizacao() == null ? "" : text(x.getLocalizacao().getBairro());
            BigDecimal value = x.getValor() == null ? BigDecimal.ZERO : x.getValor();
            return matches(filters.get("aquisicao"), x.getAquisicao()) && matches(filters.get("tipo"), x.getTipo())
                    && matches(filters.get("cidade"), city) && matches(filters.get("bairro"), neighborhood)
                    && (minimum.signum() == 0 || value.compareTo(minimum) >= 0)
                    && (maximum.signum() == 0 || value.compareTo(maximum) <= 0);
        }).toList());
        Comparator<Imovel> price = Comparator.comparing(x -> x.getValor() == null ? BigDecimal.ZERO : x.getValor());
        if ("min".equals(filters.get("ord"))) matching.sort(Comparator.<Imovel, Boolean>comparing(x -> x.getValor() == null || x.getValor().signum() == 0).thenComparing(price));
        if ("max".equals(filters.get("ord"))) matching.sort(price.reversed());
        int shown = 9;
        try { shown = Math.max(9, Math.min(9999, Integer.parseInt(parameters.getOrDefault("limite", "9")))); } catch (NumberFormatException ignored) { }
        model.addAttribute("imoveis", matching.stream().limit(shown).toList());
        model.addAttribute("total", matching.size()); model.addAttribute("temMais", matching.size() > shown);
        model.addAttribute("filtros", filters);
        model.addAttribute("localSelecionado", filters.get("cidade").isBlank() ? "" : filters.get("cidade") + "|" + filters.get("bairro"));
        UriComponentsBuilder next = UriComponentsBuilder.fromPath("/imoveis.html");
        filters.forEach((key, value) -> { if (!value.isBlank()) next.queryParam(key, value); });
        model.addAttribute("maisUrl", next.queryParam("limite", shown + 9).build().encode().toUriString());
        page(model, "imoveis", "Imóveis");
        return "imoveis";
    }

    @GetMapping("/imovel")
    String detail(@RequestParam(required = false) Long id, Model model, HttpServletResponse response) {
        Imovel x = null;
        if (id != null) {
            try { x = service.buscarImovel(id); }
            catch (ResponseStatusException e) { if (e.getStatusCode() != HttpStatus.NOT_FOUND) throw e; }
        }
        model.addAttribute("imovel", x);
        if (x == null) { response.setStatus(404); page(model, "imovel", "Imóvel não encontrado"); return "imovel"; }
        Imovel item = x;
        model.addAttribute("semelhantes", catalog(model).stream().filter(y -> !Objects.equals(y.getId(), item.getId())
                && Objects.equals(y.getTipo(), item.getTipo()) && Objects.equals(y.getAquisicao(), item.getAquisicao())).limit(3).toList());
        model.addAttribute("fotos", x.getFotos().stream().map(view::image).filter(s -> !s.isBlank()).toList());
        Map<String, Object> rows = new LinkedHashMap<>();
        rows.put("Negócio", "aluguel".equals(x.getAquisicao()) ? "Aluguel" : "Venda");
        rows.put("Tipo", view.cap(x.getTipo()));
        if (x.getLocalizacao() != null) {
            rows.put("Cidade", x.getLocalizacao().getCidade()); rows.put("Bairro", x.getLocalizacao().getBairro()); rows.put("Endereço", x.getLocalizacao().getEndereco());
        }
        rows.put("Quartos", x.getQuartos()); rows.put("Banheiros", x.getBanheiros()); rows.put("Vagas", x.getVagas());
        if (x.getAreaM2() != null && x.getAreaM2().signum() > 0) rows.put("Área", x.getAreaM2().stripTrailingZeros().toPlainString() + " m²");
        rows.values().removeIf(v -> v == null || v.toString().isBlank());
        model.addAttribute("ficha", rows);
        Institucional company = (Institucional) model.getAttribute("inst");
        model.addAttribute("foneImovel", x.getCorretor() != null && !text(x.getCorretor().getWhatsapp()).isBlank() ? x.getCorretor().getWhatsapp() : company.getWhatsapp());
        page(model, "imovel", view.title(x));
        return "imovel";
    }

    @GetMapping("/sobre") String about(Model model) { page(model, "sobre", "Sobre nós"); return "sobre"; }
    @GetMapping("/contato") String contact(Model model) { page(model, "contato", "Contato"); return "contato"; }
    @GetMapping("/corretores") String agents(Model model) { page(model, "corretores", "Corretores"); return "corretores"; }
    @GetMapping("/terreno") String land(Model model) { page(model, "terreno", "Terreno + construção"); return "terreno"; }
    private void page(Model model, String page, String title) { model.addAttribute("pagina", page); model.addAttribute("titulo", title); }
    @SuppressWarnings("unchecked") private List<Imovel> catalog(Model model) { return (List<Imovel>) model.getAttribute("catalogo"); }

    private static boolean matches(String filter, String value) { return filter.isBlank() || filter.equals(value); }

    private static String text(String value) { return value == null ? "" : value; }

    private static BigDecimal amount(String value) {
        try {
            String cleaned = value.replace("R$", "").replaceAll("\\s", "");
            if (cleaned.contains(",")) cleaned = cleaned.replace(".", "").replace(',', '.');
            else if (cleaned.matches("\\d{1,3}(?:\\.\\d{3})+")) cleaned = cleaned.replace(".", "");
            return new BigDecimal(cleaned).max(BigDecimal.ZERO);
        } catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }
}
