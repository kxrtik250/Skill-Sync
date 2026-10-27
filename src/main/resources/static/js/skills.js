const boxes=[...document.querySelectorAll('.skill-option input:not(:disabled)')];
const saveCount=document.querySelector('#save-count');
function update(){if(saveCount)saveCount.textContent=boxes.filter(x=>x.checked).length;}
boxes.forEach(x=>x.addEventListener('change',update));
const search=document.querySelector('#skill-search');
if(search)search.addEventListener('input',e=>{const q=e.target.value.trim().toLowerCase();document.querySelectorAll('.skill-option').forEach(card=>card.hidden=!card.dataset.skill.toLowerCase().includes(q));document.querySelectorAll('.skill-category').forEach(section=>section.hidden=!section.querySelector('.skill-option:not([hidden])'));});
update();
