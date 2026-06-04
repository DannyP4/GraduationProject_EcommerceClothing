export function goBack(navigate, location, fallback) {
  const backTo = location?.state?.backTo;
  const idx = (typeof window !== 'undefined' && window.history.state?.idx) || 0;
  if (backTo && idx > 0) {
    navigate(-1);
  } else {
    navigate(backTo || fallback);
  }
}
