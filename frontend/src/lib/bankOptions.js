const baseBankOptions = [
  { value: '', label: 'Selecione um banco' },
  { value: 'Banco do Brasil', label: 'Banco do Brasil' },
  { value: 'Bradesco', label: 'Bradesco' },
  { value: 'Caixa Econômica Federal', label: 'Caixa Econômica Federal' },
  { value: 'Itaú Unibanco', label: 'Itaú Unibanco' },
  { value: 'Santander', label: 'Santander' },
  { value: 'Nubank', label: 'Nubank' },
  { value: 'Inter', label: 'Inter' },
  { value: 'C6 Bank', label: 'C6 Bank' },
  { value: 'BTG Pactual', label: 'BTG Pactual' },
  { value: 'Sicredi', label: 'Sicredi' },
  { value: 'Sicoob', label: 'Sicoob' },
  { value: 'Banco Safra', label: 'Banco Safra' },
  { value: 'PagBank', label: 'PagBank' },
  { value: 'Mercado Pago', label: 'Mercado Pago' }
];

const knownBankValues = new Set(baseBankOptions.map((option) => option.value).filter(Boolean));

export const bankOptions = baseBankOptions;

export function isKnownBank(bankName) {
  return knownBankValues.has(bankName ?? '');
}

export function getBankSelectOptions(currentBankName) {
  const normalized = typeof currentBankName === 'string' ? currentBankName.trim() : '';
  if (!normalized || isKnownBank(normalized)) {
    return bankOptions;
  }

  return [{ value: normalized, label: `Banco atual: ${normalized}` }, ...bankOptions];
}
