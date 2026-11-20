export function noticeLabel(notice){
 return notice.kind==='REMINDER'?'Actividad próxima a vencer':notice.kind==='MESSAGE'?'Mensaje aprobado disponible':notice.kind==='INVITATION'?'Invitación a grupo':'Aviso de MAIN';
}
export function noticeTarget(notice){
 if(notice.kind==='REMINDER')return Number.isSafeInteger(notice.activityId)&&notice.activityId>0?'#actividad/'+notice.activityId:null;
 return ['MESSAGE','INVITATION'].includes(notice.kind)&&/^[a-f0-9-]{36}$/i.test(notice.conversationId||'')?'#chats/'+notice.conversationId:null;
}
