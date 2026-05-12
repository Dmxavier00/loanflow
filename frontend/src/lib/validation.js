function digitsOnly(value = '') {
  return value.toString().replace(/\D/g, '');
}

function allDigitsEqual(value) {
  return /^(\d)\1+$/.test(value);
}

function calculateCpfDigit(base, startingWeight) {
  const total = base.split('').reduce((sum, digit, index) => {
    return sum + Number(digit) * (startingWeight - index);
  }, 0);

  const remainder = total % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

export function normalizeCpf(value = '') {
  return digitsOnly(value).slice(0, 11);
}

export function formatCpf(value = '') {
  const digits = normalizeCpf(value);
  if (!digits) {
    return '';
  }

  if (digits.length <= 3) {
    return digits;
  }
  if (digits.length <= 6) {
    return `${digits.slice(0, 3)}.${digits.slice(3)}`;
  }
  if (digits.length <= 9) {
    return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
  }
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
}

export function isValidCpf(value = '') {
  const digits = normalizeCpf(value);
  if (digits.length !== 11 || allDigitsEqual(digits)) {
    return false;
  }

  const firstDigit = calculateCpfDigit(digits.slice(0, 9), 10);
  const secondDigit = calculateCpfDigit(`${digits.slice(0, 9)}${firstDigit}`, 11);
  return digits === `${digits.slice(0, 9)}${firstDigit}${secondDigit}`;
}

export function normalizeCep(value = '') {
  return digitsOnly(value).slice(0, 8);
}

export function formatCep(value = '') {
  const digits = normalizeCep(value);
  if (!digits) {
    return '';
  }
  if (digits.length <= 5) {
    return digits;
  }
  return `${digits.slice(0, 5)}-${digits.slice(5)}`;
}

export function isValidCep(value = '') {
  return normalizeCep(value).length === 8;
}
