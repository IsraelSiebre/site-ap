/* =====================================================================
   i18n.js — TRADUÇÃO DA INTERFACE (Português padrão, Inglês e Espanhol)
   =====================================================================
   COMO FUNCIONA
     - O site é escrito em português. Em EN/ES este arquivo troca, na tela, cada texto
       que existir no DICIONÁRIO abaixo (a comparação é pelo texto EXATO em português).
     - Ele traduz a página quando abre e continua vigiando tudo o que o JavaScript
       desenhar depois (cards, formulários, mensagens). Por isso as outras páginas
       e scripts NÃO precisam ser alterados para cada texto novo.
     - O idioma é escolhido nos botões PT | EN | ES do topo e fica guardado no
       navegador ("ap_lang"). Trocar o idioma recarrega a página.
     - Em português este arquivo não altera NADA.

   O QUE AINDA NÃO É TRADUZIDO
     - O chatbot (fica sempre em português, é ignorado de propósito: .cbox).
     - Textos digitados no painel (visão, descrições dos imóveis...) e as mensagens
       prontas que abrem no WhatsApp.

   COMO ACRESCENTAR UM TEXTO: inclua em D uma linha  "texto em português": ["English", "Español"].
   Textos com número ou nome no meio (ex.: "3 fotos") usam as REGRAS (R) mais abaixo.
   COMO ACRESCENTAR UM IDIOMA: inclua em IDIOMAS e em POS (posição na lista), acrescente a
   tradução em cada linha de D e nas regras R, e um botão em layout() (js/app.js).
   Carregado em todas as páginas públicas, ANTES do app.js.
   ===================================================================== */
(() => {
// IDIOMAS: código -> formato de números/moeda. POS: posição da tradução em cada linha do dicionário.
const IDIOMAS = { pt: "pt-BR", en: "en-US", es: "es-ES" }, POS = { en: 0, es: 1 };
let L = "pt"; try { L = localStorage.getItem("ap_lang") || "pt"; } catch (e) {}
if (!IDIOMAS[L]) L = "pt";

/* ---------- DICIONÁRIO: "português": ["inglês", "espanhol"] ---------- */
const D = {
  // menu e rodapé
  "Início": ["Home", "Inicio"], "Imóveis": ["Properties", "Inmuebles"], "Corretores": ["Agents", "Agentes"],
  "Sobre": ["About", "Nosotros"], "Sobre nós": ["About us", "Sobre nosotros"], "Contato": ["Contact", "Contacto"],
  "Terreno + construção": ["Land + construction", "Terreno + construcción"], "Menu": ["Menu", "Menú"], "Social": ["Social", "Redes"],
  "Olá! Estamos disponíveis para te ajudar.": ["Hello! We are available to help you.", "¡Hola! Estamos disponibles para ayudarle."],
  "Abrir no": ["Open in", "Abrir en"], "Fechar": ["Close", "Cerrar"], "Anterior": ["Previous", "Anterior"], "Próxima": ["Next", "Siguiente"],
  "Não foi possível carregar os dados agora.": ["Could not load the data right now.", "No fue posible cargar los datos ahora."],
  // busca e página inicial
  "Encontre o seu lar perfeito": ["Find your perfect home", "Encuentre su hogar perfecto"],
  "Negócio": ["Deal type", "Tipo de negocio"], "Selecione": ["Select", "Seleccione"], "Comprar": ["Buy", "Comprar"], "Alugar": ["Rent", "Alquilar"],
  "Tipo do imóvel": ["Property type", "Tipo de inmueble"], "Casa": ["House", "Casa"], "Terreno": ["Land", "Terreno"], "Apartamento": ["Apartment", "Apartamento"],
  "Valor mínimo": ["Minimum price", "Precio mínimo"], "Valor máximo": ["Maximum price", "Precio máximo"],
  "Cidade e bairros": ["City and neighborhoods", "Ciudad y barrios"], "Pesquisar": ["Search", "Buscar"],
  "Últimas pesquisas": ["Recent searches", "Últimas búsquedas"], "Preço não definido": ["Price not set", "Precio no definido"], "Todos": ["All", "Todos"],
  "Imóveis em destaque": ["Featured properties", "Inmuebles destacados"], "Quem somos": ["About us", "Quiénes somos"],
  "Missão": ["Mission", "Misión"], "Visão": ["Vision", "Visión"], "Valores": ["Values", "Valores"], "Leia mais": ["Read more", "Leer más"],
  "Falar com a gente": ["Talk to us", "Hable con nosotros"], "Nossos corretores": ["Our agents", "Nuestros agentes"],
  // cards e listagem
  "Venda": ["Sale", "Venta"], "Aluguel": ["Rent", "Alquiler"], "Compra": ["Purchase", "Compra"], "Ver detalhes": ["View details", "Ver detalles"],
  "Sob consulta": ["Price on request", "Precio a consultar"], "/mês": ["/month", "/mes"], "Carregando...": ["Loading...", "Cargando..."],
  "Limpar filtros": ["Clear filters", "Limpiar filtros"], "Ordenar por": ["Sort by", "Ordenar por"], "Padrão": ["Default", "Predeterminado"],
  "Menor valor": ["Lowest price", "Menor precio"], "Maior valor": ["Highest price", "Mayor precio"],
  "Ver mais imóveis": ["Show more properties", "Ver más inmuebles"],
  "Nenhum imóvel encontrado com esses filtros.": ["No properties found with these filters.", "No se encontraron inmuebles con estos filtros."],
  "Nenhum imóvel cadastrado ainda.": ["No properties registered yet.", "Aún no hay inmuebles registrados."],
  // detalhe do imóvel
  "Compartilhar no WhatsApp": ["Share on WhatsApp", "Compartir por WhatsApp"],
  "Ficha do imóvel": ["Property details", "Ficha del inmueble"], "Descrição": ["Description", "Descripción"], "Tipo": ["Type", "Tipo"],
  "Cidade": ["City", "Ciudad"], "Bairro": ["Neighborhood", "Barrio"], "Endereço": ["Address", "Dirección"], "Quartos": ["Bedrooms", "Dormitorios"],
  "Banheiros": ["Bathrooms", "Baños"], "Vagas": ["Parking spaces", "Cocheras"], "Área": ["Area", "Área"], "Localização": ["Location", "Ubicación"],
  "Falar no WhatsApp": ["Chat on WhatsApp", "Hablar por WhatsApp"], "Imóveis semelhantes": ["Similar properties", "Inmuebles similares"],
  "Imóvel não encontrado": ["Property not found", "Inmueble no encontrado"],
  "Ele pode ter sido vendido, alugado ou removido.": ["It may have been sold, rented or removed.", "Puede haber sido vendido, alquilado o retirado."],
  "Ver todos os imóveis": ["View all properties", "Ver todos los inmuebles"], "Mais fotos": ["More photos", "Más fotos"],
  // páginas institucionais
  "Empresa": ["Company", "Empresa"], "Fale com a gente": ["Get in touch", "Hable con nosotros"], "Envie uma mensagem": ["Send a message", "Envíe un mensaje"],
  "Nome": ["Name", "Nombre"], "Mensagem": ["Message", "Mensaje"], "Enviar pelo WhatsApp": ["Send via WhatsApp", "Enviar por WhatsApp"],
  "Conheça quem vai te atender": ["Meet the people who will help you", "Conozca a quienes lo atenderán"],
  "Nenhum corretor cadastrado ainda.": ["No agents registered yet.", "Aún no hay agentes registrados."],
  "Do terreno à sua casa": ["From land to your home", "Del terreno a su hogar"], "Seu terreno, a sua casa": ["Your land, your home", "Su terreno, su hogar"],
  "Conte sobre o seu terreno": ["Tell us about your land", "Cuéntenos sobre su terreno"], "Onde fica o terreno?": ["Where is the land located?", "¿Dónde queda el terreno?"]
};

/* ---------- REGRAS: textos com número ou nome no meio. f(m, i): m = trechos encontrados; i = 0 (EN) ou 1 (ES) ---------- */
const R = [
  [/^(\d+) imóveis encontrados$/, (m, i) => m[1] + [" properties found", " inmuebles encontrados"][i]],
  [/^(\d+) imóvel encontrado$/, (m, i) => m[1] + [" property found", " inmueble encontrado"][i]],
  [/^Corretor: (.+)$/, (m, i) => ["Agent: ", "Agente: "][i] + m[1]],
  [/^(\d+) fotos?$/, (m, i) => m[1] + (m[1] === "1" ? [" photo", " foto"] : [" photos", " fotos"])[i]],
  [/^(Comprar|Alugar|Todos) \| (.+)$/, (m, i) => t(m[1]) + " | " + m[2]],
  [/^Até (R\$ [\d.,]+)$/, (m, i) => ["Up to ", "Hasta "][i] + m[1]],
  [/^(©.*) - Todos os direitos reservados$/, (m, i) => m[1] + [" - All rights reserved", " - Todos los derechos reservados"][i]],
  [/^(Casa|Terreno|Apartamento) em ([^,.;:]{1,40})$/, (m, i) => t(m[1]) + [" in ", " en "][i] + m[2]]
];

// t(texto): devolve o texto traduzido (ou o mesmo texto, se não houver tradução). Espaços do começo/fim são preservados.
const t = s => {
  if (L === "pt" || s == null) return s;
  const i = POS[L], k = String(s).trim(); if (!k) return s;
  const v = D[k]; if (v) return String(s).replace(k, () => v[i]);
  for (const [re, f] of R) { const m = k.match(re); if (m) return String(s).replace(k, () => f(m, i)); }
  return s;
};

/* ---------- APLICAÇÃO NA TELA ---------- */
const ATTR = ["placeholder", "aria-label", "title"];                            // atributos que também são traduzidos
const fora = n => (n.nodeType === 3 ? n.parentElement : n).closest(".cbox,script,style,textarea,[data-no-t]");   // áreas que NÃO são traduzidas (chat)
const traduz = no => {
  if (no.nodeType === 3) { if (no.parentElement && !fora(no)) { const v = t(no.nodeValue); if (v !== no.nodeValue) no.nodeValue = v; } return; }
  if (no.nodeType !== 1 || fora(no)) return;
  [no, ...no.querySelectorAll("*")].forEach(el => ATTR.forEach(a => { if (el.hasAttribute(a) && !fora(el)) { const v = t(el.getAttribute(a)); if (v !== el.getAttribute(a)) el.setAttribute(a, v); } }));
  const w = document.createTreeWalker(no, NodeFilter.SHOW_TEXT); let n; while ((n = w.nextNode())) traduz(n);
};

// I18N: usado pelo app.js (botões de idioma e formato de moeda).
window.I18N = { L, loc: IDIOMAS[L], t, idiomas: Object.keys(IDIOMAS), set: c => { try { localStorage.setItem("ap_lang", c); } catch (e) {} location.reload(); } };

// Em EN/ES: traduz o que já está na página e vigia o que for sendo desenhado depois.
if (L !== "pt" && typeof document !== "undefined" && document.body) {
  document.documentElement.lang = L;
  traduz(document.body);
  new MutationObserver(ms => ms.forEach(m => m.addedNodes.forEach(traduz))).observe(document.body, { childList: true, subtree: true });
}
})();
