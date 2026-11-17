export function assistantRecovery(code){
 return ['NETWORK_ERROR','CANCELLED','AI_REQUEST_EXISTS'].includes(code)?'recover':'new';
}
