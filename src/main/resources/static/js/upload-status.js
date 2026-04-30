(() => {
  if (!window.batchId) return;
  const statusText = document.getElementById('statusText');
  const summaryText = document.getElementById('summaryText');
  const rowsLoaded = document.getElementById('rowsLoaded');
  const rowsSkipped = document.getElementById('rowsSkipped');
  const rowsFixed = document.getElementById('rowsFixed');
  const uploadToServerMsEl = document.getElementById('uploadToServerMs');
  const processingMsEl = document.getElementById('processingMs');
  const loading = document.getElementById('loading');
  const doneActions = document.getElementById('doneActions');
  const errorBox = document.getElementById('errorBox');

  const set = (el, v) => {
    if (!el) return;
    el.textContent = String(v ?? '');
  };

  const poll = async () => {
    try {
      const res = await fetch(`/upload/status/${batchId}/json`, { cache: 'no-store' });
      if (!res.ok) throw new Error('Could not load status');
      const j = await res.json();
      set(statusText, j.status);
      set(summaryText, j.summary);
      set(rowsLoaded, j.rowsLoaded);
      set(rowsSkipped, j.rowsSkipped);
      set(rowsFixed, j.rowsFixed);
      set(uploadToServerMsEl, j.uploadToServerMs);
      set(processingMsEl, j.processingMs);

      if (j.status === 'DONE') {
        loading.hidden = true;
        doneActions.hidden = false;
        errorBox.hidden = true;
        return;
      }
      if (j.status === 'FAILED') {
        loading.hidden = true;
        doneActions.hidden = true;
        errorBox.hidden = false;
        errorBox.textContent = `Failed: ${j.summary}${j.error ? ` (Details: ${j.error})` : ''}`;
        return;
      }
    } catch (e) {
      // ignore transient errors; keep polling
    }
    setTimeout(poll, 900);
  };

  poll();
})();

