// The development administration runs on its own origin; no credentials are shared here.
const administration=document.querySelector('#administration-link');
if(administration){const url=new URL('/admin.html',location.href);url.port='8090';administration.href=url.href;}
