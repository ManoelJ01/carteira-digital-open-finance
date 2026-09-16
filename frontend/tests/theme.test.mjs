import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const css = readFileSync(new URL('../src/styles.css', import.meta.url), 'utf8');
const tokens = Object.fromEntries([...css.matchAll(/--(color-[\w-]+):\s*(#[\da-f]{6});/g)].map(([, name, value]) => [name, value]));
function luminance(hex) {
  const rgb = hex.slice(1).match(/../g).map(channel => {
    const value = parseInt(channel, 16) / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
}

test('textos do tema mantêm contraste mínimo de 4.5:1 nas superfícies usadas', () => {
  const pairs = [
    ['text', 'surface'], ['text', 'background'], ['muted', 'surface'], ['muted', 'background'],
    ['muted', 'primary-soft'], ['muted', 'primary-subtle'], ['primary', 'surface'], ['primary', 'primary-soft'],
    ['primary-hover', 'primary-soft'], ['on-primary', 'primary'], ['on-primary', 'primary-dark'],
    ['on-primary-muted', 'primary-dark'], ['expense', 'surface'], ['expense', 'expense-soft'],
    ['on-primary', 'bank-nubank'], ['on-primary', 'bank-itau'], ['on-primary', 'bank-inter'],
  ];
  for (const [foreground, background] of pairs) {
    const values = [luminance(tokens[`color-${foreground}`]), luminance(tokens[`color-${background}`])].sort((a, b) => b - a);
    const ratio = (values[0] + .05) / (values[1] + .05);
    assert.ok(ratio >= 4.5, `${foreground}/${background}: ${ratio.toFixed(2)}:1`);
  }
});
