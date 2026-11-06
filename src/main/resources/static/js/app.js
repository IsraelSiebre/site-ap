/* Interações da interface. O HTML e os dados vêm renderizados pelo Thymeleaf. */
(() => {
  const $ = s => document.querySelector(s);
  const esc = s => String(s ?? "").replace(/[&<>"']/g, c => "&#" + c.charCodeAt(0) + ";");
  const cap = s => s ? s[0].toUpperCase() + s.slice(1) : "";
  const brl = n => n ? Number(n).toLocaleString(window.I18N ? I18N.loc : "pt-BR", {style: "currency", currency: "BRL"}) : "Sob consulta";
  const wa = (n, m) => "https://wa.me/" + String(n || "").replace(/\D/g, "") + (m ? "?text=" + encodeURIComponent(m) : "");
  window.AP = {esc, cap, brl, wa, inst: window.AP_DATA.inst, imoveis: window.AP_DATA.imoveis};

  const dismiss = () => $("#bub")?.remove();
  $("#bub button")?.addEventListener("click", dismiss);
  setTimeout(dismiss, 12000);
  document.querySelectorAll("[data-l]").forEach(button => {
    button.classList.toggle("on", button.dataset.l === I18N.L);
    button.addEventListener("click", () => I18N.set(button.dataset.l));
  });

  // Only the visitor's recent searches are stored locally; listings are always server data.
  function recentSearches() {
    try {
      const saved = JSON.parse(localStorage.getItem("ap_buscas") || "[]");
      return Array.isArray(saved) ? saved.filter(x => x && typeof x.q === "string" && typeof x.t === "string" && typeof x.s === "string").slice(0, 3).map(x => {
        const query = new URLSearchParams(x.q);
        query.delete("ref");
        return { ...x, q: query.toString() };
      }) : [];
    } catch (_) { return []; }
  }
  if ($("#ult")) {
    const recent = recentSearches();
    $("#ult").innerHTML = recent.length ? "<span>Últimas pesquisas</span>" + recent.map(x => `<a href="/imoveis?${esc(x.q)}">${esc(x.t)}<small>${esc(x.s)}</small></a>`).join("") : "";
  }
  $("#busca")?.addEventListener("submit", event => {
    const f = event.target, q = new URLSearchParams(new FormData(f));
    const [city, neighborhood] = (q.get("loc") || "|").split("|");
    q.delete("loc");
    if (city) q.set("cidade", city);
    if (neighborhood) q.set("bairro", neighborhood);
    for (const [key, value] of [...q.entries()]) if (!value) q.delete(key);
    const title = (q.get("aquisicao") === "aluguel" ? "Alugar" : q.get("aquisicao") === "compra" ? "Comprar" : "Todos") + (city ? " | " + city : "");
    const max = q.get("valorMax"), subtitle = max ? "Até " + brl(Number(max.replace(/R\$|\s/g, "").replace(/\.(?=\d{3}(?:\D|$))/g, "").replace(",", "."))) : "Preço não definido";
    try { localStorage.setItem("ap_buscas", JSON.stringify([{t: title, s: subtitle, q: q.toString()}, ...recentSearches().filter(x => x.q !== q.toString())].slice(0, 3))); } catch (_) { }
    // Native GET submission keeps the search usable when JavaScript is unavailable.
  });
  $("#lmp")?.addEventListener("click", () => { location.href = "/imoveis"; });
  $("#ord")?.addEventListener("change", () => $("#flt").requestSubmit());

  $("#fm")?.addEventListener("submit", event => {
    event.preventDefault();
    const message = document.body.dataset.p === "terreno"
      ? `Olá! Meu nome é ${$("#n").value}. Tenho interesse em um terreno em ${$("#lo").value} e quero conversar sobre construção. ${$("#m").value}`
      : `Olá! Meu nome é ${$("#n").value}. ${$("#m").value}`;
    window.open(wa(event.target.dataset.phone, message), "_blank", "noopener");
  });

  const propertyContact = $("[data-property-contact]");
  if (propertyContact) propertyContact.href += encodeURIComponent(" " + location.href);
  if ($("#share")) $("#share").href = "https://wa.me/?text=" + encodeURIComponent($("#det h1").textContent + " - " + location.href);
  const photos = window.AP_GALLERY || [], lightbox = $("#lb");
  if (lightbox && photos.length) {
    let current = 0;
    const show = index => {
      current = (index + photos.length) % photos.length;
      lightbox.querySelector("img").src = photos[current];
      lightbox.hidden = false;
    };
    document.querySelectorAll(".gi").forEach(button => button.addEventListener("click", () => show(+button.dataset.i < photos.length ? +button.dataset.i : 0)));
    lightbox.querySelector(".x").addEventListener("click", () => { lightbox.hidden = true; });
    lightbox.querySelector(".pv").addEventListener("click", () => show(current - 1));
    lightbox.querySelector(".nx").addEventListener("click", () => show(current + 1));
    document.addEventListener("keydown", event => {
      if (lightbox.hidden) return;
      if (event.key === "Escape") lightbox.hidden = true;
      if (event.key === "ArrowLeft") show(current - 1);
      if (event.key === "ArrowRight") show(current + 1);
    });
  }
})();
