(() => {
  const form = document.getElementById('uploadForm');
  const dz = document.getElementById('dropzone');
  const file = document.getElementById('file');
  const loading = document.getElementById('loading');
  const filename = document.getElementById('filename');

  const setName = () => {
    const f = file.files && file.files.length ? file.files[0] : null;
    filename.textContent = f ? `Selected: ${f.name}` : '';
  };

  file?.addEventListener('change', setName);

  const prevent = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach((ev) => {
    dz?.addEventListener(ev, prevent);
  });

  ['dragenter', 'dragover'].forEach((ev) => {
    dz?.addEventListener(ev, () => dz.classList.add('dragover'));
  });
  ['dragleave', 'drop'].forEach((ev) => {
    dz?.addEventListener(ev, () => dz.classList.remove('dragover'));
  });

  dz?.addEventListener('drop', (e) => {
    const dt = e.dataTransfer;
    if (!dt?.files?.length) return;
    file.files = dt.files;
    setName();
  });

  form?.addEventListener('submit', () => {
    loading.hidden = false;
  });
})();

