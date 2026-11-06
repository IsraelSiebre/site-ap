const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

// A minimal DOM adapter exercises the same button handlers used by the browser.
function openChat(listings) {
  class Element {
    constructor() { this.children = []; this.hidden = false; this.textContent = ''; this.html = ''; }
    append(child) { child.parent = this; this.children.push(child); }
    remove() { if (this.parent) this.parent.children = this.parent.children.filter(x => x !== this); }
    focus() { }
    set innerHTML(value) {
      this.html = value;
      this.children = [...value.matchAll(/<button type="button">([^<]*)<\/button>/g)].map(match => {
        const child = new Element(); child.textContent = match[1]; return child;
      });
    }
    get innerHTML() { return this.html; }
    querySelectorAll() { return this.children; }
  }
  const elements = Object.fromEntries(['cbtn', 'cbox', 'cfx', 'cmsg', 'copt'].map(id => [id, new Element()]));
  elements.cbox.hidden = true;
  const externalLinks = [];
  let chatMarkup = '';
  const context = {
    URLSearchParams, encodeURIComponent, setTimeout: callback => callback(),
    fetch: () => { throw new Error('Chat conversations must never issue HTTP requests'); },
    location: {href: ''},
    document: {
      body: {insertAdjacentHTML(position, html) { chatMarkup = html; }},
      querySelector: selector => elements[selector.replace('#', '')] || null,
      createElement: () => new Element(), addEventListener() { }
    },
    window: {
      open: (...args) => externalLinks.push(args),
      AP: {
        esc: value => String(value ?? '').replace(/[&<>"']/g, c => '&#' + c.charCodeAt(0) + ';'),
        brl: value => value ? 'R$ ' + value : 'Sob consulta',
        cap: value => value[0].toUpperCase() + value.slice(1),
        wa: (phone, message) => 'https://wa.me/' + phone + '?text=' + encodeURIComponent(message),
        inst: {nome: 'Empresa de teste', whatsapp: '5519999999999', terrenoConstrucao: {texto: 'Construímos seu sonho.', whatsapp: '5519888888888'}},
        imoveis: listings
      }
    }
  };
  vm.runInNewContext(readFileSync(join(__dirname, '../../main/resources/static/js/chat.js'), 'utf8'), context);
  return {
    open: () => elements.cbtn.onclick(),
    choose: text => {
      const button = elements.copt.children.find(x => x.textContent === text);
      assert.ok(button, 'Expected option: ' + text);
      return button.onclick();
    },
    messages: () => elements.cmsg.children.map(x => x.textContent || x.innerHTML).join('\n'),
    markup: () => chatMarkup,
    options: () => elements.copt.children.map(x => x.textContent),
    links: externalLinks,
    destination: () => context.location.href
  };
}

const catalog = [
  {id: 101, tipo: 'casa', aquisicao: 'compra', valor: 190000, localizacao: {cidade: 'Capivari', bairro: 'Centro'}},
  {id: 102, tipo: 'casa', aquisicao: 'compra', valor: 630000, localizacao: {cidade: 'Capivari', bairro: 'Centro'}},
  {id: 103, tipo: 'casa', aquisicao: 'aluguel', valor: 1200, localizacao: {cidade: 'Capivari', bairro: 'Centro'}},
  {id: 104, tipo: 'casa', aquisicao: 'compra', valor: 0, localizacao: {cidade: 'Capivari', bairro: 'Centro'}}
];

test('chat applies purchase, location and maximum-price filters entirely in the browser', async () => {
  const chat = openChat(catalog);
  await chat.open();
  assert.match(chat.messages(), /Empresa de teste/);
  for (const answer of ['Comprar', 'Casa', 'Centro - Capivari', 'Até R$ 200 mil']) await chat.choose(answer);
  assert.match(chat.messages(), /Encontrei 1 imóvel/);
  assert.match(chat.messages(), /\/imovel\?id=101/);
  assert.doesNotMatch(chat.messages(), /\/imovel\?id=10[234]/);
  await chat.choose('Ver todos os resultados');
  assert.match(chat.destination(), /aquisicao=compra/);
  assert.match(chat.destination(), /valorMax=200000/);
});

test('chat supports empty results, restart and the separate construction WhatsApp', async () => {
  const chat = openChat([]);
  await chat.open();
  for (const answer of ['Alugar', 'Casa', 'Qualquer lugar', 'Qualquer valor']) await chat.choose(answer);
  assert.match(chat.messages(), /Não encontrei imóveis/);
  await chat.choose('Refinar busca');
  await chat.choose('Terreno + construção');
  assert.match(chat.messages(), /Construímos seu sonho/);
  await chat.choose('Ainda não tenho terreno');
  await chat.choose('Falar sobre terreno + construção');
  assert.match(chat.links[0][0], /^https:\/\/wa.me\/5519888888888/);
  assert.match(decodeURIComponent(chat.links[0][0]), /Quero encontrar um terreno/);
});

test('chat has no text entry and construction location is selected from options', async () => {
  const chat = openChat(catalog);
  assert.doesNotMatch(chat.markup(), /<(input|textarea|form)\b|contenteditable/i);
  await chat.open();
  await chat.choose('Terreno + construção');
  assert.doesNotMatch(chat.messages(), /Digite/i);
  assert.deepEqual(Array.from(chat.options()), ['Centro - Capivari', 'Outro local', 'Ainda não tenho terreno', 'Voltar ao início']);
  await chat.choose('Centro - Capivari');
  assert.match(chat.messages(), /Anotado: terreno em Centro - Capivari/);
  await chat.choose('Falar sobre terreno + construção');
  assert.match(decodeURIComponent(chat.links[0][0]), /Tenho um terreno em Centro - Capivari/);
});

test('other construction locations can be discussed via WhatsApp', async () => {
  const chat = openChat([]);
  await chat.open();
  await chat.choose('Terreno + construção');
  await chat.choose('Outro local');
  assert.match(chat.messages(), /localização pelo WhatsApp/);
  await chat.choose('Falar sobre terreno + construção');
  assert.match(decodeURIComponent(chat.links[0][0]), /Tenho um terreno e quero conversar/);
});
