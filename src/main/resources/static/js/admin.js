/* Interações do painel SSR. Os formulários e dados são renderizados pelo Thymeleaf. */
(() => {
  const $ = selector => document.querySelector(selector);
  document.querySelectorAll('[data-href]').forEach(button => {
    button.addEventListener('click', () => { location.href = button.dataset.href; });
  });
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!confirm(form.dataset.confirm)) event.preventDefault();
    });
  });
  const modal = $('.md');
  if (modal) {
    modal.addEventListener('click', event => {
      if (event.target === modal) location.href = modal.dataset.closeUrl;
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') location.href = modal.dataset.closeUrl;
    });
  }
  const toast = $('#toast');
  if (toast && toast.classList.contains('on')) setTimeout(() => { toast.className = ''; }, 5000);

  const gallery = $('[data-gallery]');
  if (gallery) {
    const thumbnails = $('#ths');
    // Os arquivos continuam locais até o envio multipart do formulário.
    const sync = () => {
      const transfer = new DataTransfer();
      let index = 0;
      thumbnails.querySelectorAll('.tt').forEach((thumbnail, position) => {
        if (thumbnail.uploadFile) {
          transfer.items.add(thumbnail.uploadFile);
          thumbnail.querySelector('input').value = 'n:' + index++;
        }
        const actions = thumbnail.querySelector('span');
        const remove = thumbnail.querySelector('[data-remove]');
        if (position === 0) actions.replaceChildren(document.createTextNode('Capa'), remove);
        else {
          const cover = document.createElement('button');
          cover.type = 'button';
          cover.dataset.cover = '';
          cover.textContent = 'Capa';
          actions.replaceChildren(cover, remove);
        }
      });
      gallery.files = transfer.files;
    };
    thumbnails.addEventListener('click', event => {
      const thumbnail = event.target.closest('.tt');
      if (!thumbnail) return;
      if (event.target.closest('[data-cover]')) thumbnails.prepend(thumbnail);
      if (event.target.closest('[data-remove]')) {
        if (thumbnail.previewUrl) URL.revokeObjectURL(thumbnail.previewUrl);
        thumbnail.remove();
      }
      sync();
    });
    gallery.addEventListener('change', () => {
      for (const file of Array.from(gallery.files)) {
        const thumbnail = document.createElement('div');
        thumbnail.className = 'tt';
        thumbnail.uploadFile = file;
        thumbnail.previewUrl = URL.createObjectURL(file);
        const img = document.createElement('img');
        img.src = thumbnail.previewUrl;
        img.alt = file.name;
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'ordemFotos';
        const actions = document.createElement('span');
        const cover = document.createElement('button');
        cover.type = 'button';
        cover.dataset.cover = '';
        cover.textContent = 'Capa';
        const remove = document.createElement('button');
        remove.type = 'button';
        remove.dataset.remove = '';
        remove.textContent = '×';
        remove.setAttribute('aria-label', 'Remover foto');
        actions.append(cover, remove);
        thumbnail.append(img, input, actions);
        thumbnails.append(thumbnail);
      }
      sync();
    });
    sync();
  }

  const single = $('[data-single-photo]');
  if (single) {
    const thumbnails = $('#ths');
    let previewUrl;
    thumbnails.addEventListener('click', event => {
      if (!event.target.closest('[data-single-remove]')) return;
      $('#removerFoto').value = 'true';
      single.value = '';
      thumbnails.replaceChildren();
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    });
    single.addEventListener('change', () => {
      const file = single.files[0];
      if (!file) return;
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = URL.createObjectURL(file);
      const thumbnail = document.createElement('div');
      thumbnail.className = 'tt';
      const img = document.createElement('img');
      img.src = previewUrl;
      img.alt = 'Foto do corretor';
      const actions = document.createElement('span');
      actions.append('Foto');
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.dataset.singleRemove = '';
      remove.textContent = '×';
      remove.setAttribute('aria-label', 'Remover foto');
      actions.append(remove);
      thumbnail.append(img, actions);
      thumbnails.replaceChildren(thumbnail);
      $('#removerFoto').value = 'false';
    });
  }

  const banner = $('#bpv');
  if (banner) {
    const picker = $('#bf'), shade = $('#be');
    let background = banner.dataset.background || '', previewUrl;
    const preview = () => {
      $('#bv').textContent = shade.value;
      $('#bov').style.opacity = Number(shade.value) / 100;
      banner.style.backgroundImage = background ? 'url(' + JSON.stringify(background) + ')' : '';
    };
    picker.addEventListener('change', () => {
      const file = picker.files[0];
      if (!file) return;
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = URL.createObjectURL(file);
      background = previewUrl;
      $('#removerBanner').value = 'false';
      preview();
    });
    $('#brm').addEventListener('click', () => {
      background = '';
      picker.value = '';
      $('#removerBanner').value = 'true';
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      preview();
    });
    shade.addEventListener('input', preview);
    preview();
  }
})();
