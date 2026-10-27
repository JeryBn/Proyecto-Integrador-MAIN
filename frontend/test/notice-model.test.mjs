import test from 'node:test';import assert from 'node:assert/strict';
import {noticeLabel,noticeTarget} from '../src/notice-model.mjs';
test('recordatorio abre actividad y no una conversación nula',()=>{
 const notice={kind:'REMINDER',activityId:9001,conversationId:null};assert.equal(noticeTarget(notice),'#actividad/9001');assert.equal(noticeLabel(notice),'Actividad próxima a vencer');
});
test('destinos inválidos y tipos futuros no producen enlaces arbitrarios',()=>{
 for(const activityId of [null,-1,'javascript:alert(1)',1.5,Number.MAX_SAFE_INTEGER+1])assert.equal(noticeTarget({kind:'REMINDER',activityId}),null);
 assert.equal(noticeTarget({kind:'INVITATION',conversationId:'https://example.com'}),null);
 assert.equal(noticeTarget({kind:'UNKNOWN',activityId:1}),null);
});
test('mensajes e invitaciones conservan destino de chat',()=>{
 const id='00000000-0000-0000-0000-000000000001';assert.equal(noticeTarget({kind:'MESSAGE',conversationId:id}),'#chats/'+id);assert.equal(noticeLabel({kind:'INVITATION'}),'Invitación a grupo');
});
