(() => {
  const slider = document.getElementById('discountPct');
  const label = document.getElementById('discountLabel');
  const set = () => {
    if (!slider || !label) return;
    label.textContent = `Discount: ${slider.value}%`;
  };
  slider?.addEventListener('input', set);
  set();
})();

