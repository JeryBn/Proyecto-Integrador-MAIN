export function statusTextColor(background){
 const color=/^#[a-f0-9]{6}$/i.test(background)?background:'#087f68';
 const rgb=[1,3,5].map(at=>parseInt(color.slice(at,at+2),16)/255).map(v=>v<=.04045?v/12.92:((v+.055)/1.055)**2.4);
 const light=.2126*rgb[0]+.7152*rgb[1]+.0722*rgb[2];
 return (light+.05)/.05>=1.05/(light+.05)?'#000000':'#ffffff';
}
export function ownerPlaylist(feed,owner){return feed.filter(s=>s.owner===owner).sort((a,b)=>Date.parse(a.createdAt)-Date.parse(b.createdAt)||a.id.localeCompare(b.id));}
export function canPublish(role){return role==='Alumno'||role==='Docente';}
