package br.com.andrepacheco.imoveis.controller;

import br.com.andrepacheco.imoveis.domain.Imovel;
import br.com.andrepacheco.imoveis.domain.Institucional;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Formatting only: templates escape all company and listing text with th:text. */
@Component("view")
public class ViewHelper {
    public String brl(BigDecimal value) {
        return value == null || value.signum() == 0 ? "Sob consulta"
                : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value);
    }
    public String cap(String value) {
        return value == null || value.isEmpty() ? "" : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
    public String initial(String value) { return value == null || value.isBlank() ? "?" : value.substring(0, 1); }
    public String wa(String number) { return wa(number, ""); }
    public String wa(String number, String message) {
        return "https://wa.me/" + text(number).replaceAll("\\D", "")
                + (message == null || message.isBlank() ? "" : "?text=" + encode(message));
    }
    public String phone(String value) {
        String n = text(value).replaceAll("\\D", "").replaceFirst("^55", "");
        return n.length() < 10 ? n : "(" + n.substring(0, 2) + ") " + n.substring(2, n.length() - 4) + "-" + n.substring(n.length() - 4);
    }
    public String instagram(String value) { return "https://instagram.com/" + encode(text(value).replace("@", "")); }
    public String map(String address, String app) {
        String q = encode(text(address).replaceAll("\\s[–-]\\s", ", "));
        return switch (app) {
            case "waze" -> "https://waze.com/ul?q=" + q + "&navigate=yes";
            case "apple" -> "https://maps.apple.com/?q=" + q;
            default -> "https://www.google.com/maps/search/?api=1&query=" + q;
        };
    }
    public List<String> paragraphs(String value) {
        return Arrays.stream(text(value).split("\\r?\\n\\s*\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
    public List<String> lines(String value) { return Arrays.asList(text(value).split("\\r?\\n", -1)); }
    public String location(Imovel x) {
        if (x.getLocalizacao() == null) return "";
        return Stream.of(x.getLocalizacao().getBairro(), x.getLocalizacao().getCidade())
                .filter(s -> s != null && !s.isBlank()).collect(java.util.stream.Collectors.joining(" - "));
    }
    public String title(Imovel x) {
        String bairro = x.getLocalizacao() == null ? "" : text(x.getLocalizacao().getBairro());
        return cap(x.getTipo()) + (bairro.isBlank() ? "" : " em " + bairro);
    }
    public String image(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.matches("https?://[^\\s\"'<>]+") || value.matches("/?(?:img|uploads)/[\\w./-]+"))
            return value.startsWith("img/") || value.startsWith("uploads/") ? "/" + value : value;
        return "";
    }
    public String heroStyle(Institucional company) {
        String url = image(company.getBannerFundo());
        if (url.isBlank()) return "";
        int darkness = company.getBannerEscuro() == null ? 55 : Math.max(0, Math.min(85, company.getBannerEscuro()));
        return "--hero-img:url(\"" + url.replace("(", "%28").replace(")", "%29").replace("\\", "%5C")
                + "\");--hero-escuro:" + darkness / 100.0;
    }
    public String terrainPhone(Institucional company) {
        return company.getTerrenoConstrucao() != null && !text(company.getTerrenoConstrucao().getWhatsapp()).isBlank()
                ? company.getTerrenoConstrucao().getWhatsapp() : company.getWhatsapp();
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private static String text(String value) { return value == null ? "" : value; }
}
