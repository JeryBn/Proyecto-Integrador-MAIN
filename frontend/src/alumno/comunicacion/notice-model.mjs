export function noticeLabel(notice){
 if(notice.kind==='CONTACT_REQUEST')return 'Solicitud de contacto';
 return notice.kind==='REMINDER'?'Actividad próxima a vencer':notice.kind==='MESSAGE'?'Mensaje aprobado disponible':notice.kind==='INVITATION'?'Invitación a grupo':'Aviso de COLEGIA';
}
export function noticeTarget(notice){
 if(notice.kind==='CONTACT_REQUEST')return '#perfil';
 if(notice.kind==='REMINDER')return Number.isSafeInteger(notice.activityId)&&notice.activityId>0?'#actividad/'+notice.activityId:null;
 return ['MESSAGE','INVITATION'].includes(notice.kind)&&/^[a-f0-9-]{36}$/i.test(notice.conversationId||'')?'#chats/'+notice.conversationId:null;
}
