import React,{useEffect} from 'react';
import data from './stitch-templates.json';
import './stitch-utilities.css';
export const textSlot=value=>({tag,attrs})=>React.createElement(tag,attrs,value);
export function Original({node,slots={},bindings={}}){
 if(typeof node==='string')return bindings[node.trim()]??node;
 if(!node)return null;
 const tag=node.tag==='body'?'div':node.tag,attrs={...node.attrs};
 const children=()=>node.children?.map((n,i)=><Original key={i} node={n} slots={slots} bindings={bindings}/>);
 if(node.slot&&Object.hasOwn(slots,node.slot))return typeof slots[node.slot]==='function'?slots[node.slot]({tag,attrs,children,node}):slots[node.slot];
 if(node.slot==='avatar')return <span className={(attrs.className||'')+' stitch-initial'} aria-hidden="true">C</span>;
 if(tag==='button'&&!attrs.type)attrs.type='button';
 if(tag==='a'&&attrs.href==='#')attrs.onClick=e=>e.preventDefault();
 if(tag==='input'){
  if(attrs.value!==undefined){attrs.defaultValue=attrs.value;delete attrs.value;}
  return React.createElement(tag,attrs);
 }
 if(['img','br','hr','wbr'].includes(tag))return React.createElement(tag,attrs);
 return React.createElement(tag,attrs,children());
}
export function StitchScreen({name,slots,bindings}){
 useEffect(()=>{
  const body=document.body,previous=body.dataset.stitchScreen;
  const classes=(name==='login'?'bg-surface font-body-md text-on-surface min-h-screen flex flex-col justify-between selection:bg-secondary-fixed selection:text-on-secondary-fixed':'bg-background font-body-md text-on-surface antialiased selection:bg-secondary-fixed selection:text-on-secondary-fixed').split(' ');
  const added=classes.filter(value=>!body.classList.contains(value));
  body.classList.add(...added);body.dataset.stitchScreen=name;
  return()=>{body.classList.remove(...added);if(previous===undefined)delete body.dataset.stitchScreen;else body.dataset.stitchScreen=previous;};
 },[name]);
 return <div className="stitch" data-screen={name}><Original node={data.screens[name]} slots={slots} bindings={bindings}/></div>;
}
export function Material({children}){return <span className="material-symbols-outlined" aria-hidden="true">{children}</span>;}
export function OriginalTask({activity}){return <Original node={data.blocks.task} bindings={{'MATEMÁTICAS':activity.curso,'Taller 04: Derivadas':activity.titulo,'Vence mañana':activity.vencida?'Vencida':'Pendiente','23:59 hrs':new Intl.DateTimeFormat('es-PE',{dateStyle:'medium',timeStyle:'short',timeZone:'America/Lima'}).format(new Date(activity.fechaLimite+'-05:00'))}} slots={{}}/>;}
