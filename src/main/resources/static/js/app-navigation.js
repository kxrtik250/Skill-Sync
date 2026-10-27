(() => {
  const nav = document.querySelector('.app aside nav');
  if (!nav) return;
  const current = window.location.pathname.replace(/\/$/, '') || '/dashboard';
  for (const link of nav.querySelectorAll('a[href]')) {
    const target = new URL(link.href, window.location.origin).pathname.replace(/\/$/, '');
    const selected = target === current;
    link.classList.toggle('active', selected);
    if (selected) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  }
})();
