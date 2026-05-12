import test from 'node:test';
import assert from 'node:assert/strict';
import { getBankSelectOptions, isKnownBank } from './bankOptions.js';
import {
  getAuthenticatedSecondaryAction,
  getBankAccountStatusLabel,
  hasBankAccount,
  isBankAccountPending,
  requiresBankAccount
} from './bankAccountPolicy.js';

test('requiresBankAccount should require an account for borrower', () => {
  assert.equal(requiresBankAccount({ papel: 'SOLICITANTE' }), true);
});

test('requiresBankAccount should require an account for lender', () => {
  assert.equal(requiresBankAccount({ papel: 'CREDOR' }), true);
});

test('requiresBankAccount should not require an account for admin only', () => {
  assert.equal(requiresBankAccount({ papel: 'ADMIN' }), false);
});

test('hasBankAccount detects a registered account', () => {
  assert.equal(hasBankAccount({ contaBancariaId: 99 }), true);
  assert.equal(hasBankAccount({ contaBancariaId: null }), false);
});

test('isBankAccountPending combines role and account state', () => {
  assert.equal(isBankAccountPending({ papel: 'SOLICITANTE', contaBancariaId: null }), true);
  assert.equal(isBankAccountPending({ papel: 'SOLICITANTE', contaBancariaId: 10 }), false);
  assert.equal(isBankAccountPending({ papel: 'ADMIN', contaBancariaId: null }), false);
});

test('getAuthenticatedSecondaryAction sends a pending user to account setup', () => {
  assert.deepEqual(
    getAuthenticatedSecondaryAction({ papel: 'CREDOR', contaBancariaId: null }),
    { to: '/minha-conta', label: 'Cadastrar conta banc\u00e1ria' }
  );
});

test('getAuthenticatedSecondaryAction enables a new proposal when account exists', () => {
  assert.deepEqual(
    getAuthenticatedSecondaryAction({ papel: 'SOLICITANTE', contaBancariaId: 22 }),
    { to: '/solicitacoes', label: 'Nova proposta' }
  );
});

test('getBankAccountStatusLabel reflects the expected state', () => {
  assert.equal(getBankAccountStatusLabel({ papel: 'ADMIN' }), 'OPCIONAL');
  assert.equal(getBankAccountStatusLabel({ papel: 'SOLICITANTE', contaBancariaId: null }), 'PENDENTE');
  assert.equal(getBankAccountStatusLabel({ papel: 'CREDOR', contaBancariaId: 3 }), 'CADASTRADA');
});

test('isKnownBank recognizes catalog banks', () => {
  assert.equal(isKnownBank('Banco do Brasil'), true);
  assert.equal(isKnownBank('Banco Inventado'), false);
});

test('getBankSelectOptions preserves a legacy bank outside the catalog', () => {
  const options = getBankSelectOptions('Banco Legado');
  assert.equal(options[0].value, 'Banco Legado');
  assert.equal(options[0].label, 'Banco atual: Banco Legado');
});
