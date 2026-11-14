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
 video:<><rect x="3" y="5" width="14" height="14" rx="2"/><path d="m17 10 4-2v8l-4-2"/></>,
 mic:<><rect x="9" y="3" width="6" height="12" rx="3"/><path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21M9 21h6"/></>,
 arrowLeft:<path d="m15 18-6-6 6-6"/>,
 more:<><circle cx="5" cy="12" r="1" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1" fill="currentColor" stroke="none"/><circle cx="19" cy="12" r="1" fill="currentColor" stroke="none"/></>,
 lock:<><rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></>,
 paperclip:<path d="m8.5 12.5 6.1-6.1a3 3 0 0 1 4.2 4.2l-8.5 8.5a5 5 0 0 1-7.1-7.1l8.2-8.2"/>,
 send:<path d="m3 3 18 9-18 9 3.8-9L3 3Zm3.8 9H21"/>,
 check:<path d="m5 12 4 4L19 6"/>,
 spark:<path d="M12 2c.8 5.2 2.8 7.2 8 8-5.2.8-7.2 2.8-8 8-.8-5.2-2.8-7.2-8-8 5.2-.8 7.2-2.8 8-8Z"/>,
 home:<><path d="m3 10 9-7 9 7"/><path d="M5 9v11h14V9M9 20v-6h6v6"/></>,
 book:<><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v17H6.5A2.5 2.5 0 0 0 4 22V5.5Z"/><path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v17h4.5A2.5 2.5 0 0 1 20 22V5.5Z"/></>,
 clipboard:<><rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4.5V3h6v1.5M9 10h6M9 14h6M9 18h4"/></>,
 user:<><circle cx="12" cy="8" r="4"/><path d="M4.5 21a7.5 7.5 0 0 1 15 0"/></>,
 bell:<><path d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></>,
 calendar:<><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18M7 14h3M14 14h3M7 18h3"/></>,
 chart:<><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></>,
 clock:<><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
 arrowRight:<path d="m9 18 6-6-6-6"/>,
 chevronDown:<path d="m6 9 6 6 6-6"/>,
 graduation:<><path d="m2 9 10-5 10 5-10 5L2 9Z"/><path d="M6 11.5V16c3.5 2.7 8.5 2.7 12 0v-4.5M22 9v6"/></>,
 file:<><path d="M6 2h8l4 4v16H6V2Z"/><path d="M14 2v5h5M9 12h6M9 16h6"/></>,
 checkCircle:<><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
 alert:<><path d="M12 3 2.8 20h18.4L12 3Z"/><path d="M12 9v5M12 17.5v.5"/></>,
 layers:<><path d="m12 2 9 5-9 5-9-5 9-5Z"/><path d="m3 12 9 5 9-5M3 17l9 5 9-5"/></>,
 logout:<><path d="M10 4H4v16h6M14 8l4 4-4 4M8 12h10"/></>
};

export function Icon({name,size=20,className=''}){
 return <svg className={'ui-icon '+className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">{paths[name]}</svg>;
}
