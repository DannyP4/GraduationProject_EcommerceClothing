export function colorLabel(t, raw) {
  if (!raw) return raw;
  return t(`colors.${raw.toLowerCase()}`, { defaultValue: raw });
}

export function variantLabel(t, label) {
  if (!label) return label;
  return label.split(' / ').map((tok) => colorLabel(t, tok)).join(' / ');
}
