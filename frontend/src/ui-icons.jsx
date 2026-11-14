import React from 'react';

const paths={
 plus:<path d="M12 5v14M5 12h14"/>,
 search:<><circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4"/></>,
 refresh:<><path d="M20 7v5h-5"/><path d="M4 17v-5h5"/><path d="M6.1 8.2A7 7 0 0 1 18.7 7M5.3 17A7 7 0 0 0 17.9 15.8"/></>,
 message:<path d="M5 5.8h14v10.4H9l-4 3V5.8Z"/>,
 users:<><path d="M16 20v-1.5a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4V20"/><circle cx="9.5" cy="7" r="3.5"/><path d="M17 10a3 3 0 0 0 0-6M21 20v-1.5a4 4 0 0 0-3-3.8"/></>,
 shield:<path d="M12 3 5.5 5.8v5.1c0 4.2 2.7 7.9 6.5 9.1 3.8-1.2 6.5-4.9 6.5-9.1V5.8L12 3Z"/>,
 status:<><circle cx="12" cy="12" r="7"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2"/></>,
 pencil:<><path d="m4 20 4.2-1 10.4-10.4-3.2-3.2L5 15.8 4 20Z"/><path d="m13.8 7 3.2 3.2"/></>,
 camera:<><rect x="3" y="6.5" width="18" height="13" rx="2"/><path d="m8 6.5 1.4-2h5.2l1.4 2"/><circle cx="12" cy="13" r="3.5"/></>,
 arrowLeft:<path d="m15 18-6-6 6-6"/>,
 more:<><circle cx="5" cy="12" r="1" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1" fill="currentColor" stroke="none"/><circle cx="19" cy="12" r="1" fill="currentColor" stroke="none"/></>,
 lock:<><rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></>,
 paperclip:<path d="m8.5 12.5 6.1-6.1a3 3 0 0 1 4.2 4.2l-8.5 8.5a5 5 0 0 1-7.1-7.1l8.2-8.2"/>,
 send:<path d="m3 3 18 9-18 9 3.8-9L3 3Zm3.8 9H21"/>,
 check:<path d="m5 12 4 4L19 6"/>,
 spark:<path d="M12 2c.8 5.2 2.8 7.2 8 8-5.2.8-7.2 2.8-8 8-.8-5.2-2.8-7.2-8-8 5.2-.8 7.2-2.8 8-8Z"/>
};

export function Icon({name,size=20,className=''}){
 return <svg className={'ui-icon '+className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">{paths[name]}</svg>;
}
