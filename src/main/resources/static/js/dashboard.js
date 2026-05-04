(() => {
  if (!chartModel || !chartModel.months) return;

  const months = Array.from(chartModel.months);
  const monthRevenue = Array.from(chartModel.monthRevenue);

  const categories = Array.from(chartModel.categories);
  const categoryRevenue = Array.from(chartModel.categoryRevenue);

  const regions = Array.from(chartModel.regions);
  const regionRevenue = Array.from(chartModel.regionRevenue);

  const scatter = (chartModel.discountScatter || []).map((p) => ({ x: p.x, y: p.y }));

  const moneyTicks = (v) => {
    if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(1)}M`;
    if (v >= 1_000) return `$${(v / 1_000).toFixed(1)}k`;
    return `$${Math.round(v)}`;
  };

  const baseOpts = {
    responsive: true,
    plugins: {
      legend: { display: false },
      tooltip: { enabled: true }
    }
  };

  new Chart(document.getElementById('salesOverTime'), {
    type: 'line',
    data: {
      labels: months,
      datasets: [{
        label: 'Revenue',
        data: monthRevenue,
        borderColor: '#4f8cff',
        backgroundColor: 'rgba(79,140,255,.18)',
        tension: 0.35,
        fill: true
      }]
    },
    options: {
      ...baseOpts,
      scales: {
        y: {
          ticks: { callback: moneyTicks }
        }
      }
    }
  });

  new Chart(document.getElementById('topCategories'), {
    type: 'bar',
    data: {
      labels: categories,
      datasets: [{
        label: 'Revenue',
        data: categoryRevenue,
        backgroundColor: 'rgba(34,197,94,.40)',
        borderColor: 'rgba(34,197,94,.85)',
        borderWidth: 1
      }]
    },
    options: {
      ...baseOpts,
      indexAxis: 'y',
      scales: {
        x: { ticks: { callback: moneyTicks } }
      }
    }
  });

  new Chart(document.getElementById('salesByRegion'), {
    type: 'bar',
    data: {
      labels: regions,
      datasets: [{
        label: 'Revenue',
        data: regionRevenue,
        backgroundColor: 'rgba(251,191,36,.35)',
        borderColor: 'rgba(251,191,36,.85)',
        borderWidth: 1
      }]
    },
    options: {
      ...baseOpts,
      scales: {
        y: { ticks: { callback: moneyTicks } }
      }
    }
  });

  new Chart(document.getElementById('discountImpact'), {
    type: 'scatter',
    data: {
      datasets: [{
        label: 'Rows',
        data: scatter,
        pointRadius: 3,
        pointBackgroundColor: 'rgba(239,68,68,.75)'
      }]
    },
    options: {
      ...baseOpts,
      scales: {
        x: { title: { display: true, text: 'Discount %' } },
        y: { title: { display: true, text: 'Units sold' } }
      }
    }
  });
})();

