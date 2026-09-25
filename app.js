const lessons={restaurant:[["I'd like a cup of coffee, please.","Listen first, then repeat naturally."],["Could I have the menu, please?","Ask politely for the menu."],["I'd like a large coffee without sugar.","Order something with a detail."],["Could I have a glass of water, please?","Make a polite request."],["What do you recommend?","Ask the waiter for a recommendation."],["I'd like the chicken sandwich with a side salad.","Order a meal clearly."],["Could you make that without onions, please?","Ask politely for a change."],["Is this dish spicy?","Ask about the food."],["Could we have the bill, please?","Ask for the bill politely."],["Can I pay by card?","Ask about payment."],["Everything was delicious. Thank you.","Give polite feedback."],["That's all, thank you. Have a nice day.","Finish the conversation politely."]],daily:[["I usually wake up at seven o'clock.","Talk about your routine."],["I brush my teeth and get ready for work.","Describe your morning routine."],["I am going to work now.","Use the present continuous."],["I usually have breakfast before I leave home.","Talk about a regular habit."],["I had breakfast before I left home.","Talk about something you already did."],["I am feeling a little tired today.","Describe how you feel today."],["I have a lot of work to finish today.","Talk about your plans for today."],["I usually come home at around seven.","Talk about your evening routine."],["I am going to watch a movie tonight.","Talk about a future plan."],["I'll call you when I get home.","Talk about a future action."],["I had a busy day, but everything went well.","Describe your day."],["I am trying to improve my English every day.","Talk about your learning goal."]],interview:[["Tell me about yourself.","Answer as if you are in an interview."],["I have three years of work experience.","Give information about your experience."],["I enjoy learning new skills.","Talk about something positive."],["I am a hardworking and responsible person.","Describe your qualities."],["I work well both independently and in a team.","Talk about teamwork."],["I always try to complete my work on time.","Describe your work habit."],["My biggest strength is that I learn quickly.","Talk about a strength."],["I am working on improving my communication skills.","Talk about an area you are improving."],["I handled customer queries and prepared reports in my previous role.","Describe your experience."],["I can stay calm when I have a difficult task.","Describe how you handle challenges."],["I want this job because it matches my skills and experience.","Explain your interest in the role."],["Do you have any questions for me?","Practice ending an interview politely."]],travel:[["Could you tell me where the station is?","Ask for directions politely."],["I'd like to book a room for two nights.","Make a hotel request."],["What time does the train leave?","Ask about a schedule."],["Could you help me with my luggage?","Ask for help politely."],["How much is a ticket to the city centre?","Ask about a ticket price."],["Where can I buy a bus ticket?","Ask where to buy a ticket."],["Is there a bus to the airport?","Ask about transport."],["Could you tell me how long the journey takes?","Ask about travel time."],["I have a reservation under the name Sandeep.","Check in at a hotel."],["Could I have a room with a quiet view?","Make a hotel request."],["What time is breakfast served?","Ask about hotel facilities."],["Thank you for your help. Have a great day.","Finish the travel conversation politely."]]};

let scenario="restaurant",index=0,correct=0,attempted=0,recognition=null,freeRecognition=null,freeRunning=false,freeSeconds=60,timer=null;
const $=id=>document.getElementById(id);
const current=()=>lessons[scenario][index][0];
function normalizeSpeechText(s){
  let t=s.toLowerCase().replace(/[^a-z0-9\s']/g," ").replace(/\s+/g," ").trim();
  // Speech recognition sometimes spells common words letter-by-letter: "w e r e".
  const spelled={
    "w e r e":"were","w a s":"was","a m":"am","i s":"is","a r e":"are",
    "h e":"he","s h e":"she","i t":"it","i":"i","h a s":"has","h a v e":"have",
    "d o":"do","d o n t":"dont","d o e s":"does","g o":"go","g o e s":"goes",
    "t h e":"the","a n":"an","a":"a","t o":"to"
  };
  Object.keys(spelled).sort((a,b)=>b.length-a.length).forEach(k=>{
    t=t.replace(new RegExp("\\b"+k.replace(/ /g,"\\s+")+"\\b","gi"),spelled[k]);
  });
  return t.replace(/\s+/g," ").trim();
}
const normalize=s=>normalizeSpeechText(s);
const words=s=>normalize(s).split(" ").filter(Boolean);

function similarity(a,b){
  const A=words(a),B=words(b),used=new Set();
  if(!A.length)return 0;
  let hits=0;
  A.forEach(w=>{const i=B.findIndex((x,j)=>x===w&&!used.has(j));if(i>=0){hits++;used.add(i)}});
  return hits/Math.max(A.length,B.length);
}

function commonGrammar(text){
  text=normalizeSpeechText(text);
  const fixes=[];
  const add=(bad,good,reason,priority=50)=>fixes.push({bad,good,reason,priority});
  const first=(re,handler)=>{
    const m=text.match(re);
    if(m) handler(m);
  };
  const baseForm=(v)=>{
    v=v.toLowerCase();
    if(v==="went") return "go";
    if(v==="gone") return "go";
    if(v==="was"||v==="were") return "be";
    if(v==="has"||v==="had") return "have";
    if(v==="did") return "do";
    if(v.endsWith("ies") && v.length>3) return v.slice(0,-3)+"y";
    if(v.endsWith("es") && v.length>3) return v.slice(0,-2);
    if(v.endsWith("s") && v.length>2) return v.slice(0,-1);
    return v;
  };
  const thirdForm=(v)=>{
    v=v.toLowerCase();
    if(/(s|x|z|ch|sh|o)$/.test(v)) return v+"es";
    if(/[^aeiou]y$/.test(v)) return v.slice(0,-1)+"ies";
    return v+"s";
  };

  // 1. Subject + be agreement.
  const beRules=[
    [/\b(i)\s+(is|are|was|were)\b/gi,"I am","Use am with I."],
    [/\b(you|we|they)\s+(is|was)\b/gi,null,"Use are/were with you, we and they."],
    [/\b(you|we|they)\s+(has)\b/gi,null,"Use have with you, we and they."],
    [/\b(he|she|it)\s+(are|were)\b/gi,null,"Use is/was with he, she and it."],
    [/\b(he|she|it)\s+(have)\b/gi,null,"Use has with he, she and it."]
  ];
  beRules.forEach(([re,forced,reason])=>{
    let m;
    while((m=re.exec(text))){
      const s=m[1].toLowerCase(),v=m[2].toLowerCase();
      const map={i:{is:"am",are:"am",was:"am",were:"am"},you:{is:"are",was:"were",has:"have"},we:{is:"are",was:"were",has:"have"},they:{is:"are",was:"were",has:"have"},he:{are:"is",were:"was",have:"has"},she:{are:"is",were:"was",have:"has"},it:{are:"is",were:"was",have:"has"}};
      const good=forced?forced.toLowerCase().replace(/^i /,"I "):s+" "+map[s][v];
      add(m[0],good,reason,100);
    }
  });

  // 2. Missing be before adjective, location, nationality or possessive phrase.
  first(/\b(i|you|he|she|it|we|they)\s+(my|your|his|her|our|their)\s+[a-z]+\b/i,m=>{
    const s=m[1].toLowerCase(),be=s==="i"?"am":/^(you|we|they)$/.test(s)?"are":"is";
    add(m[0],s+" "+be+" "+m[2]+" "+m[3],"A subject pronoun needs a form of be before this complement.",100);
  });
  const commonAdj="happy|sad|tired|busy|ready|late|early|hungry|thirsty|angry|happy|fine|good|bad|sick|well|right|wrong|sure|afraid|available|free|married|single|late|here|there";
  const subjectAdj=new RegExp("\\b(i|you|he|she|it|we|they)\\s+("+commonAdj+")\\b","gi");
  let m;
  while((m=subjectAdj.exec(text))){
    const s=m[1].toLowerCase();
    if(/^(am|are|is|was|were)$/.test(m[2].toLowerCase())) continue;
    const be=s==="i"?"am":/^(you|we|they)$/.test(s)?"are":"is";
    add(m[0],s+" "+be+" "+m[2],"Use the correct form of be before an adjective or state.",75);
  }

  // 3. Modals + base verb.
  const modal=/\b(can|could|may|might|must|shall|should|will|would)\s+(to\s+)?([a-z]+)\b/gi;
  while((m=modal.exec(text))){
    const modalWord=m[1],verb=m[3].toLowerCase();
    if(m[2]) add(m[0],modalWord+" "+baseForm(verb),"A modal is followed directly by the base verb, without to.",100);
    else if(verb!==baseForm(verb) && verb!=="is" && verb!=="are" && verb!=="was" && verb!=="were"){
      add(m[0],modalWord+" "+baseForm(verb),"After a modal verb, use the base form.",95);
    }
  }

  // 4. Do/does/did + base verb.
  const aux=/\b(do|does|did)\s+([a-z]+)\b/gi;
  while((m=aux.exec(text))){
    const v=m[2].toLowerCase();
    if(!/^(not|so|that|this)$/.test(v) && v!==baseForm(v))
      add(m[0],m[1]+" "+baseForm(v),"After do/does/did, use the base form of the verb.",95);
  }

  // 5. Third-person simple present for common action verbs.
  const commonVerbs="go|come|work|live|like|love|want|need|know|think|play|eat|drink|watch|read|write|speak|help|call|start|finish|open|close|make|take|give|tell|ask|use|try|study|learn|drive|walk|run|cook|sleep|feel|look|seem|remember|forget|understand|enjoy|prefer|visit|travel|meet|buy|pay|send|bring|keep|find|work|rain";
  const third=new RegExp("\\b(he|she|it)\\s+("+commonVerbs+")\\b","gi");
  while((m=third.exec(text))){
    const v=m[2].toLowerCase();
    add(m[0],m[1]+" "+thirdForm(v),"In the simple present, he/she/it normally takes the third-person singular form.",80);
  }

  // 6. Perfect tenses: have/has/had + past participle.
  const irregular={went:"gone",ate:"eaten",saw:"seen",did:"done",made:"made",took:"taken",gave:"given",came:"come",ran:"run",spoke:"spoken",wrote:"written",broke:"broken",chose:"chosen",knew:"known",began:"begun",drank:"drunk",drove:"driven",forgot:"forgotten",found:"found",got:"gotten",kept:"kept",left:"left",read:"read",said:"said",sent:"sent",sang:"sung",slept:"slept",swam:"swum",thought:"thought",told:"told",understood:"understood",wore:"worn"};
  const perfect=/\b(has|have|had)\s+([a-z]+)\b/gi;
  while((m=perfect.exec(text))){
    const v=m[2].toLowerCase();
    if(irregular[v]) add(m[0],m[1]+" "+irregular[v],"After have/has/had, use the past participle.",95);
    else if(/^[a-z]+ed$/.test(v)===false && /^(go|eat|see|do|take|give|speak|write|break|choose|know|begin|drink|drive|forget|find|leave|send|sing|swim|think|tell|wear)$/.test(v))
      add(m[0],m[1]+" "+v+"ed","Use a past participle after have/has/had.",60);
  }

  // 7. Continuous tenses: be + -ing.
  const continuous=/\b(am|is|are|was|were)\s+([a-z]+)\b/gi;
  while((m=continuous.exec(text))){
    const v=m[2].toLowerCase();
    const auxiliaries=["am","is","are","was","were","been"];
    if(!auxiliaries.includes(v) && !/ing$/.test(v) && /^(go|come|work|live|eat|drink|sleep|run|walk|study|learn|talk|speak|write|read|watch|play|cook|drive|wait|look|listen|try|travel|rain|rain)$/.test(v))
      add(m[0],m[1]+" "+v+"ing","Use the -ing form after am/is/are/was/were for a continuous action.",75);
  }

  // 8. Infinitives after common verbs.
  const infinitive=/\b(want|need|plan|hope|try|decide|learn|promise|agree|refuse|expect|offer|choose|would like)\s+([a-z]+)\b/gi;
  while((m=infinitive.exec(text))){
    const v=m[2].toLowerCase();
    if(!/^(to|is|are|was|were|am|have|has|had|not)$/.test(v))
      add(m[0],m[1]+" to "+v,"This verb normally takes to + the base verb here.",70);
  }

  // 9. Gerund after common prepositions.
  const prepIng=/\b(after|before|without|by|instead of)\s+([a-z]+)\b/gi;
  while((m=prepIng.exec(text))){
    const v=m[2].toLowerCase();
    if(/^(go|come|eat|drink|work|study|learn|talk|speak|drive|walk|run|wait|sleep|watch|read|write)$/.test(v))
      add(m[0],m[1]+" "+v+"ing","After this preposition, use a gerund (-ing form).",55);
  }

  // 10. Articles: a/an and common vowel-sound exceptions.
  const articleWords={apple:"an",orange:"an",hour:"an",honest:"an",heir:"an",honor:"an",university:"a",uniform:"a",unicorn:"a",useful:"a",user:"a",European:"a",one:"a"};
  const article=/\b(a|an)\s+([a-z]+)\b/gi;
  while((m=article.exec(text))){
    const noun=m[2].toLowerCase(),wanted=articleWords[noun]||null;
    if(wanted && m[1].toLowerCase()!==wanted)
      add(m[0],wanted+" "+m[2],"Choose a/an according to the sound at the beginning of the next word.",80);
  }

  // 11. Count/non-count noun quantifiers.
  const uncountable="advice|information|furniture|luggage|equipment|news|homework|work|money|traffic|knowledge|research";
  const quant=new RegExp("\\b(many|few|a few|several|these|those|two|three)\\s+("+uncountable+")\\b","gi");
  while((m=quant.exec(text))) add(m[0],m[1]+" "+m[2],"This noun is normally uncountable in standard English; use a quantity expression such as some or a piece of.",65);
  const singularCount=new RegExp("\\b(much|little|a little)\\s+([a-z]+)\\b","gi");
  while((m=singularCount.exec(text))){
    if(/^(people|students|friends|cars|books|things|questions|days|years|jobs|ideas)$/.test(m[2].toLowerCase()))
      add(m[0],m[1]==="much"?"many "+m[2]:"a few "+m[2],"Use many/few with plural countable nouns.",65);
  }

  // 12. Demonstratives + noun number.
  first(/\b(this|that)\s+(people|students|friends|cars|books|things)\b/i,m=>add(m[0],(m[1].toLowerCase()==="this"?"these ":"those ")+m[2],"Use these/those with plural nouns.",70));
  first(/\b(these|those)\s+(person|student|friend|car|book|thing)\b/i,m=>add(m[0],(m[1].toLowerCase()==="these"?"this ":"that ")+m[2],"Use this/that with a singular noun.",70));

  // 13. There is/are and there was/were.
  first(/\bthere\s+(is|was)\s+(people|children|men|women|students|friends|cars|books|things)\b/i,m=>{
    const v=m[1].toLowerCase()==="is"?"are":"were";
    add(m[0],"there "+v+" "+m[2],"The verb agrees with the plural noun after there.",90);
  });

  // 14. Negative agreement.
  first(/\b(he|she|it)\s+(don't|do not)\b/i,m=>add(m[0],m[1]+" doesn't","Use doesn't with he, she and it.",90));
  first(/\b(i|you|we|they)\s+(doesn't)\b/i,m=>add(m[0],m[1]+" don't","Use don't with I, you, we and they.",90));

  // 15. Pronoun case after a verb/preposition.
  const objectCase=/\b(call|help|see|meet|invite|tell|ask|give|send|show)\s+(i|he|she|we|they)\b/gi;
  const obj={i:"me",he:"him",she:"her",we:"us",they:"them"};
  while((m=objectCase.exec(text))) add(m[0],m[1]+" "+obj[m[2].toLowerCase()],"Use an object pronoun after this verb.",55);
  const prepCase=/\b(with|for|to|from|between|about|beside|without)\s+(i|he|she|we|they)\b/gi;
  while((m=prepCase.exec(text))) add(m[0],m[1]+" "+obj[m[2].toLowerCase()],"Use an object pronoun after a preposition.",55);

  // 16. Possessive pronoun/article confusion.
  const possessive={my:"mine",your:"yours",his:"his",her:"hers",our:"ours",their:"theirs"};
  const poss=/\b(my|your|his|her|our|their)\s+(is|are|was|were)\b/gi;
  while((m=poss.exec(text))) add(m[0],possessive[m[1].toLowerCase()]+" "+m[2],"Use a possessive pronoun when the noun is omitted.",50);

  // 17. Comparatives and superlatives.
  first(/\bmore\s+(better|worse|faster|slower|bigger|smaller|stronger|weaker|higher|lower|easier|harder|older|younger|closer|farther)\b/i,m=>add(m[0],m[1],"Do not combine more with an adjective that already has a comparative form.",80));
  first(/\bmost\s+(best|worst|fastest|slowest|biggest|smallest|strongest|weakest|highest|lowest|easiest|hardest|oldest|youngest)\b/i,m=>add(m[0],m[1],"Do not combine most with an adjective that already has a superlative form.",80));
  first(/\b(as)\s+(better|worse|bigger|smaller|faster|slower)\s+as\b/i,m=>add(m[0],"as "+m[2].replace(/er$/,"")+" as","Use the base adjective in as...as comparisons.",55));

  // 18. Common preposition errors.
  const prepMap={
    "interested on":"interested in","good in":"good at","good on":"good at","afraid from":"afraid of",
    "depend of":"depend on","listen music":"listen to music","married with":"married to",
    "different than":"different from","arrive to":"arrive at","discuss about":"discuss",
    "enter into":"enter"
  };
  Object.keys(prepMap).forEach(bad=>{
    const re=new RegExp("\\b"+bad.replace(/ /g,"\\s+")+"\\b","i");
    const hit=text.match(re);
    if(hit) add(hit[0],prepMap[bad],"Use the standard preposition or verb pattern here.",45);
  });

  // 19. Common verb-pattern errors.
  first(/\b(enjoy|avoid|finish|keep|mind)\s+(to)\s+([a-z]+)\b/i,m=>add(m[0],m[1]+" "+m[3]+"ing","These verbs are followed by a gerund, not to + verb.",70));
  first(/\b(let|make)\s+(me|him|her|us|them)\s+to\s+([a-z]+)\b/i,m=>add(m[0],m[1]+" "+m[2]+" "+m[3],"Let/make + object is followed by the base verb.",70));

  // 20. Common conjunction/connector errors.
  first(/\b(because|although|even though)\s+but\b/i,m=>add(m[0],m[1],"Do not normally use because/although together with but for the same contrast/cause.",60));
  first(/\b(despite|in spite of)\s+(he|she|they|we|i)\b/i,m=>add(m[0],m[1]+" "+({he:"his",she:"her",they:"their",we:"our",i:"my"}[m[2].toLowerCase()]||"the")+" presence","Despite/in spite of is followed by a noun phrase or gerund.",45));

  // 21. Common double negatives in standard English.
  first(/\b(don't|doesn't|didn't|can't|couldn't|won't|wouldn't|never)\s+([a-z]+)\s+no\b/i,m=>add(m[0],m[0].replace(/\s+no\b/i,""),"Avoid two negative markers when standard English needs one.",40));

  // 22. Question word order.
  const question=/^(where|when|why|how|what|who)\s+(you|he|she|they|we|i)\s+([a-z]+)\??$/i;
  first(question,m=>{
    const s=m[2].toLowerCase();
    const aux=/^(i|you|we|they)$/.test(s)?"do":"does";
    if(/^(was|were|is|are|can|will|should|could|would|did|do|does|have|has|had)$/.test(m[3].toLowerCase())) return;
    add(m[0],m[1]+" "+aux+" "+m[2]+" "+m[3]+"?","In a normal present-tense question, put the auxiliary before the subject.",75);
  });

  // 23. Subject-verb agreement for frequent plural/singular nouns.
  const pluralNouns="people|children|men|women|students|friends|cars|books|things|questions|ideas|problems|days|years|jobs";
  const singularWrong=new RegExp("\\b("+pluralNouns+")\\s+(is|was|has)\\b","gi");
  while((m=singularWrong.exec(text))){
    const good=m[2].toLowerCase()==="is"?"are":m[2].toLowerCase()==="was"?"were":"have";
    add(m[0],m[1]+" "+good,"The plural subject needs the plural verb form.",90);
  }
  const singularNouns="person|child|man|woman|student|friend|car|book|thing|question|idea|problem|day|year|job";
  const pluralWrong=new RegExp("\\b("+singularNouns+")\\s+(are|were|have)\\b","gi");
  while((m=pluralWrong.exec(text))){
    const good=m[2].toLowerCase()==="are"?"is":m[2].toLowerCase()==="were"?"was":"has";
    add(m[0],m[1]+" "+good,"The singular subject needs the singular verb form.",90);
  }

  // 24. Common adjective/adverb confusion.
  first(/\b(run|drive|speak|work|sing|dance)\s+(good|bad|slow|quick|easy)\b/i,m=>{
    const adverb={good:"well",bad:"badly",slow:"slowly",quick:"quickly",easy:"easily"}[m[2].toLowerCase()];
    add(m[0],m[1]+" "+adverb,"Use an adverb to describe how an action is performed.",40);
  });

  // 25. Capitalize the first character of the sentence as a presentation correction.
  // Speech normalization lowercases text, so do not flag sentence-initial "I"
  // or generate capitalization findings from the normalized copy.
  // Harper receives the original spoken text separately and can handle real
  // capitalization issues without this local rule creating false positives.

  // Keep the most useful corrections first and remove exact duplicates.
  const seen=new Set();
  return fixes.filter(x=>{
    const k=x.bad.toLowerCase()+"|"+x.good.toLowerCase();
    if(seen.has(k)) return false;
    seen.add(k);
    return true;
  }).sort((a,b)=>b.priority-a.priority);
}
function grammarHTML(fixes){
  if(fixes.length)return "<div class='grammar-title'>📝 Grammar suggestion</div>"+fixes.map(x=>"❌ <strong>"+x.bad+"</strong> → <strong>"+x.good+"</strong>").join("<br>");
  return "<div class='grammar-title'>📝 Grammar check</div>✅ No common grammar mistake detected. Your sentence looks okay.";
}

function render(){
  $("promptText").textContent=current();
  $("hint").textContent=lessons[scenario][index][1];
  $("score").textContent=correct+"/"+attempted;
  $("bar").style.width=(index/lessons[scenario].length*100)+"%";
  $("feedback").className="feedback hidden";
  $("heard").className="heard hidden";
  $("status").textContent="Ready.";
}

function speakText(text){
  if(!("speechSynthesis"in window))return $("status").textContent="Text-to-speech is not supported.";
  speechSynthesis.cancel();
  const u=new SpeechSynthesisUtterance(text);u.lang="en-US";u.rate=.9;speechSynthesis.speak(u);
}

$("scenario").onchange=e=>{scenario=e.target.value;index=0;correct=0;attempted=0;render()};
$("listen").onclick=()=>speakText(current());

function startRecognition(){
  const SR=window.SpeechRecognition||window.webkitSpeechRecognition;
  if(!SR)return $("status").textContent="Speech recognition is not supported. Try Chrome or Edge.";
  if(recognition)recognition.stop();
  recognition=new SR();
  recognition.lang="en-US";
  recognition.interimResults=false;
  recognition.maxAlternatives=1;
  recognition.onstart=()=>{$("status").textContent="🎤 Listening... speak now."};
  recognition.onerror=e=>{$("status").textContent="Could not hear you ("+e.error+"). Try again."};
  recognition.onresult=e=>evaluate(e.results[0][0].transcript);
  recognition.start();
}
$("speak").onclick=startRecognition;

function evaluate(text){
  attempted++;
  const score=similarity(current(),text),fixes=commonGrammar(text);
  $("heard").className="heard";
  $("heard").innerHTML="<b>You said:</b> "+text;
  const fb=$("feedback");
  fb.className="feedback "+(score>=.82?"good":"bad");
  if(score>=.82){
    correct++;
    fb.innerHTML="✅ <b>Good!</b> Your sentence matches well.<br><br>"+grammarHTML(fixes);
    $("status").textContent="Nice! Grammar feedback is shown below.";
  }else{
    fb.innerHTML="Keep going. <b>Better sentence:</b> <strong>"+current()+"</strong><br><br>"+grammarHTML(fixes);
    $("status").textContent="Your grammar feedback is shown below.";
  }
  $("score").textContent=correct+"/"+attempted;
}

$("retry").onclick=()=>{speakText(current());setTimeout(startRecognition,700)};
$("next").onclick=()=>{index=(index+1)%lessons[scenario].length;render()};

function freeStart(){
  const SR=window.SpeechRecognition||window.webkitSpeechRecognition;
  if(!SR){$("freeFeedback").className="feedback bad";$("freeFeedback").textContent="Speech recognition is not supported in this browser.";return}
  freeRunning=true;freeSeconds=60;$("freeTimer").textContent=60;$("freeText").textContent="";$("freeFeedback").className="feedback hidden";
  freeRecognition=new SR();freeRecognition.lang="en-US";freeRecognition.continuous=true;freeRecognition.interimResults=true;
  freeRecognition.onresult=e=>{let out="";for(let i=0;i<e.results.length;i++)out+=e.results[i][0].transcript+" ";$("freeText").textContent=out.trim()};
  freeRecognition.onend=()=>{if(freeRunning)try{freeRecognition.start()}catch(e){}};
  freeRecognition.start();
  timer=setInterval(()=>{freeSeconds--;$("freeTimer").textContent=freeSeconds;if(freeSeconds<=0)freeStop()},1000);
  $("freeSpeak").textContent="⏹ Stop challenge";
}

function freeStop(){
  if(!freeRunning)return;
  freeRunning=false;clearInterval(timer);try{freeRecognition.stop()}catch(e){}
  $("freeSpeak").textContent="🎤 Start 60-second challenge";
  const text=$("freeText").textContent,normalized=normalizeSpeechText(text),fixes=commonGrammar(normalized),fb=$("freeFeedback");
  if(!normalized || normalized.split(/\s+/).filter(Boolean).length<3){
    fb.className="feedback bad";
    fb.innerHTML="⚠️ <b>I couldn't understand enough of your speech to check the grammar.</b><br>Try speaking one complete sentence clearly, then continue.";
    return;
  }
  fb.className="feedback "+(fixes.length?"bad":"good");
  fb.innerHTML=fixes.length?"<b>Practice these grammar corrections:</b><br>"+fixes.map(x=>"❌ "+x.bad+" → <strong>"+x.good+"</strong>").join("<br>")+"<br><br>Now say your idea again.":grammarHTML(fixes)+"<br><br>Keep speaking and add more detail.";
}
$("freeSpeak").onclick=()=>freeRunning?freeStop():freeStart();

// Standalone spoken grammar checker.
function grammarCheckSentence(raw){
  // Speech recognition can occasionally hand us text with unusual Unicode
  // spacing/characters. Keep a safe fallback so a valid spoken sentence is
  // never reported as "Please say a complete sentence".
  const source=String(raw||"").trim();
  let text=normalizeSpeechText(source);
  const sourceWords=source.split(/\s+/).filter(Boolean);
  if(text.split(/\s+/).filter(Boolean).length<2 && sourceWords.length>=2){
    text=source.toLowerCase().replace(/[^a-z0-9\s']/g," ").replace(/\s+/g," ").trim();
  }
  const fixes=commonGrammar(text);
  let corrected=text;
  fixes.forEach(x=>{
    if(x.bad && x.good && x.bad!==x.good){
      corrected=corrected.replace(x.bad,x.good);
    }
  });
  return {text,fixes,corrected};
}
let grammarLastCorrection="";
let grammarPendingText="";
let grammarRecognition=null;

function resetGrammarSpeech(){
  try{if(grammarRecognition)grammarRecognition.stop()}catch(e){}
  grammarRecognition=null;
  grammarPendingText="";
  grammarLastCorrection="";
  $("grammarLive").className="heard hidden";
  $("grammarLiveText").textContent="";
  $("grammarHeard").className="heard hidden";
  $("grammarHeard").innerHTML="";
  $("grammarResult").className="feedback hidden";
  $("grammarResult").innerHTML="";
  $("grammarListen").disabled=true;
  $("grammarSend").disabled=true;
  $("grammarRespeak").disabled=false;
  $("grammarStatus").textContent="Ready. Tap Respeak and say your sentence again.";
}

function finishGrammarCapture(){
  $("grammarSpeak").disabled=false;
  $("grammarSpeak").textContent="🎤 Speak a sentence";
  $("grammarRespeak").disabled=false;
  $("grammarSend").disabled=grammarPendingText.split(/\s+/).filter(Boolean).length<2;
  if(grammarPendingText.trim()){
    $("grammarStatus").textContent="Speech captured. If it is wrong, tap Respeak. Otherwise tap Check sentence.";
  }
}

function startGrammarTool(){
  const SR=window.SpeechRecognition||window.webkitSpeechRecognition;
  if(!SR){
    $("grammarStatus").textContent="Speech recognition is not supported. Please use Chrome or Edge.";
    return;
  }
  const btn=$("grammarSpeak");
  const send=$("grammarSend");
  btn.disabled=true;
  send.disabled=true;
  $("grammarRespeak").disabled=true;
  $("grammarListen").disabled=true;
  btn.textContent="🎤 Listening...";
  $("grammarStatus").textContent="Listening... speak your sentence.";
  $("grammarLive").className="heard";
  $("grammarLiveText").textContent="";
  grammarPendingText="";

  const rec=new SR();
  grammarRecognition=rec;
  rec.lang="en-US";
  rec.interimResults=true;
  rec.continuous=true;
  rec.maxAlternatives=1;

  rec.onresult=e=>{
    let finalText="";
    let interimText="";
    for(let i=0;i<e.results.length;i++){
      const t=e.results[i][0].transcript;
      if(e.results[i].isFinal) finalText+=t+" ";
      else interimText+=t+" ";
    }
    if(finalText.trim()) grammarPendingText=(grammarPendingText+" "+finalText).trim();
    const shown=(grammarPendingText+" "+interimText).trim();
    $("grammarLiveText").textContent=shown;
    send.disabled=grammarPendingText.split(/\s+/).filter(Boolean).length<2;
    $("grammarRespeak").disabled=false;
  };

  rec.onerror=e=>{
    $("grammarStatus").textContent="Could not hear you ("+e.error+"). Tap Respeak and try again.";
    finishGrammarCapture();
  };

  rec.onend=()=>{
    if(btn.disabled){
      finishGrammarCapture();
    }
  };

  rec.start();
}

async function harperCheckSentence(text){
  if(!window.harperLinter)return {available:false,lints:[]};
  try{
    if(window.harperReady) await window.harperReady;
    const lints=await window.harperLinter.lint(text);
    return {available:true,lints};
  }catch(e){
    return {available:false,lints:[]};
  }
}

async function lingoCheckSentence(text){
  try{
    if(window.ltGrammarReady) await window.ltGrammarReady;
    if(!window.ltGrammarEngine)return {available:false,matches:[]};
    const data=JSON.parse(window.ltGrammarEngine.check_json(text));
    return {available:true,matches:Array.isArray(data.matches)?data.matches:[]};
  }catch(e){
    console.warn("LingoTweaker check failed.",e);
    return {available:false,matches:[]};
  }
}

function lingoResult(text,matches){
  const findings=[];
  let corrected=text;
  const ordered=[...matches].sort((a,b)=>(b.range?.start||0)-(a.range?.start||0));
  const byteToIndex=(str,byteOffset)=>{
    if(!byteOffset)return 0;
    let bytes=0;
    for(let i=0;i<str.length;i++){
      bytes+=new TextEncoder().encode(str[i]).length;
      if(bytes>=byteOffset)return i+1;
    }
    return str.length;
  };
  ordered.forEach(match=>{
    const range=match.range||{};
    const start=byteToIndex(text,range.start||0);
    const end=byteToIndex(text,range.end||0);
    const wrong=text.slice(start,end);
    const suggestions=Array.isArray(match.suggestions)?match.suggestions:[];
    const suggestion=suggestions[0];
    const good=suggestion&&typeof suggestion.value==="string"?suggestion.value:"";
    findings.push({
      wrong,
      good,
      message:match.message||"LanguageTool grammar rule detected a possible issue.",
      source:"LingoTweaker"
    });
    if(good) corrected=corrected.slice(0,start)+good+corrected.slice(end);
  });
  findings.reverse();
  return {corrected,findings};
}

function harperResult(text,lints){
  let corrected=text;
  const findings=[];
  [...lints].sort((a,b)=>b.span().start-a.span().start).forEach(lint=>{
    const span=lint.span();
    const suggestions=lint.suggestions();
    const suggestion=suggestions&&suggestions.length?suggestions[0]:null;
    const wrong=text.slice(span.start,span.end);
    const good=suggestion?suggestion.get_replacement_text():"";
    findings.push({wrong,good,message:lint.message(),source:"Harper"});
    if(suggestion) corrected=corrected.slice(0,span.start)+good+corrected.slice(span.end);
  });
  findings.reverse();
  return {corrected,findings};
}

async function showGrammarToolResult(raw){
  const result=grammarCheckSentence(raw);
  const heard=$("grammarHeard"),box=$("grammarResult");
  heard.className="heard";
  heard.innerHTML="<b>You said:</b> "+raw;
  if(!result.text || result.text.split(/\s+/).filter(Boolean).length<2){
    box.className="feedback bad";
    box.innerHTML="⚠️ Please say a complete sentence so I can check it.";
    return;
  }

  box.className="feedback";
  box.innerHTML="🔎 Checking grammar locally...";
  $("grammarStatus").textContent="Checking your sentence on your device...";

  // LingoTweaker is the only external grammar engine used here.
  // It is a local LanguageTool-derived WASM engine; no grammar text is sent
  // to a remote grammar server. Our systematic rules remain active as well.
  const lt=await lingoCheckSentence(raw.trim());
  const localFixes=result.fixes;
  let findings=[];
  let corrected=result.text;
  let engineLabel="Systematic local grammar engine";

  if(lt.available){
    const checked=lingoResult(raw.trim(),lt.matches);
    findings=checked.findings;
    if(!localFixes.length) corrected=checked.corrected;
    engineLabel="LingoTweaker (LanguageTool-derived) + systematic grammar engine";
  }

  localFixes.forEach(x=>{
    const duplicate=findings.some(f=>f.wrong.toLowerCase()===x.bad.toLowerCase() && f.good.toLowerCase()===x.good.toLowerCase());
    if(!duplicate) findings.push({wrong:x.bad,good:x.good,message:x.reason,source:"Grammar engine"});
    if(x.bad && x.good && x.bad!==x.good){
      const escaped=x.bad.replace(/[.*+?^()$|[\\]\\]/g,"\\\\$&");
      corrected=corrected.replace(new RegExp(escaped,"i"),x.good);
    }
  });

  // Never report capitalization-only findings for spoken sentences.
  // Speech recognition casing is presentation data, not a grammar mistake.
  findings=findings.filter(x=>{
    const wrong=(x.wrong||"").trim();
    const good=(x.good||"").trim();
    return !(wrong.toLowerCase()!==good.toLowerCase() && /^[a-z]+$/.test(wrong) && /^[A-Z][a-z]*$/.test(good) && wrong.toLowerCase()===good.toLowerCase());
  });

  grammarLastCorrection=corrected;
  $("grammarListen").disabled=false;

  if(findings.length){
    box.className="feedback bad";
    box.innerHTML="<div class='grammar-title'>❌ Mistake found</div><div class='correction'><b>Better sentence:</b> "+corrected+"</div>"+
      findings.map(x=>"<div>❌ <strong>"+x.wrong+"</strong> → <strong>"+(x.good||"remove")+"</strong><br><small>"+x.message+"</small></div>").join("<br>")+
      "<div class='explanation'>Checked on your device using "+engineLabel+". Your sentence is not sent to a grammar server.</div>";
    $("grammarStatus").textContent="Grammar feedback found.";
  }else{
    box.className="feedback good";
    box.innerHTML="<div class='grammar-title'>✅ Looks good</div><div class='correction'>"+raw.trim()+"</div><div class='explanation'>No issue was detected by "+engineLabel+". Your sentence is not sent to a grammar server.</div>";
    $("grammarStatus").textContent="No issue detected.";
  }
}
$("grammarSpeak").onclick=startGrammarTool;
$("grammarRespeak").onclick=resetGrammarSpeech;
$("grammarSend").onclick=()=>{
  if(!grammarPendingText.trim())return;
  try{if(grammarRecognition)grammarRecognition.stop()}catch(e){}
  const text=grammarPendingText.trim();
  $("grammarStatus").textContent="Checking your sentence...";
  showGrammarToolResult(text);
  $("grammarSend").disabled=true;
  $("grammarRespeak").disabled=false;
};
render();