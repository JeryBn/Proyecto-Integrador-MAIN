import {test} from 'node:test';
import assert from 'node:assert/strict';
import {assistantRecovery} from '../src/assistant-model.mjs';
test('una espera cancelada o respuesta incierta recupera la misma clave',()=>{for(const code of ['NETWORK_ERROR','CANCELLED','AI_REQUEST_EXISTS'])assert.equal(assistantRecovery(code),'recover');});
test('cuota agotada y errores confirmados requieren otra solicitud explícita',()=>{for(const code of ['AI_QUOTA_EXHAUSTED','AI_ACCESS_DENIED','AI_NOT_CONFIGURED','AI_RATE_LIMIT','AI_DAILY_LIMIT','AI_INCOMPLETE'])assert.equal(assistantRecovery(code),'new');});
