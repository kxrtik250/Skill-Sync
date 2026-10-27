const cards=[...document.querySelectorAll('.career-row')];
let active='all';
const search=document.querySelector('#career-search');
const value=id=>document.querySelector(id)?.value||'all';
function filter(){
 const q=(search?.value||'').trim().toLowerCase(),domain=value('#career-domain'),intensity=value('#career-intensity'),level=value('#career-level'),industry=value('#career-industry'),work=value('#career-workstyle'),minimum=Number(value('#career-match'));
 let shown=0;
 for(const card of cards){const ok=(active==='all'||card.dataset.type===active)&&(domain==='all'||card.dataset.domain===domain)&&(intensity==='all'||card.dataset.intensity===intensity)&&(level==='all'||card.dataset.level===level)&&(industry==='all'||card.dataset.industry===industry)&&(work==='all'||card.dataset.workStyle===work)&&Number(card.dataset.score)>=minimum&&card.dataset.search.includes(q);card.hidden=!ok;if(ok)shown++;}
 const empty=document.querySelector('#no-careers');if(empty)empty.hidden=shown>0;
}
document.querySelectorAll('.filter-tabs button').forEach(b=>b.addEventListener('click',()=>{active=b.dataset.type;document.querySelectorAll('.filter-tabs button').forEach(x=>x.classList.toggle('filter-active',x===b));filter();}));
search?.addEventListener('input',filter);
document.querySelectorAll('#career-domain,#career-intensity,#career-level,#career-industry,#career-workstyle,#career-match').forEach(el=>el.addEventListener('change',filter));
