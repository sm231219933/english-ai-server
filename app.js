const lessons={restaurant:[["I'd like a cup of coffee, please.","Listen first, then repeat naturally."],["Could I have the menu, please?","Ask politely for the menu."],["I'd like a large coffee without sugar.","Order something with a detail."],["That's all, thank you.","Finish your order politely."]],daily:[["I usually wake up at seven o'clock.","Talk about your routine."],["I am going to work now.","Use the present continuous."],["I had breakfast before I left home.","Talk about something you already did."],["I'll call you when I get home.","Talk about a future action."]],interview:[["Tell me about yourself.","Answer as if you are in an interview."],["I have three years of work experience.","Give information about your experience."],["I enjoy learning new skills.","Talk about something positive."],["Why do you want this job?","Give a short, natural answer."]],travel:[["Could you tell me where the station is?","Ask for directions politely."],["I'd like to book a room for two nights.","Make a hotel request."],["What time does the train leave?","Ask about a schedule."],["Could you help me with my luggage?","Ask for help politely."]]};

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
  const add=(bad,good,reason)=>fixes.push({bad,good,reason});

  // 1) Modal/auxiliary verbs always take a base-form main verb.
  const modal=/\\b(can|could|may|might|must|shall|should|will|would)\\s+([a-z]+)\\b/gi;
  let m;
  while((m=modal.exec(text))){
    const v=m[2].toLowerCase();
    if(v.endsWith("ies")) add(m[0],m[1]+" "+v.slice(0,-3)+"y","After a modal verb, use the base form of the verb.");
    else if(v.endsWith("es")) add(m[0],m[1]+" "+v.slice(0,-2),"After a modal verb, use the base form of the verb.");
    else if(v.endsWith("s")) add(m[0],m[1]+" "+v.slice(0,-1),"After a modal verb, use the base form of the verb.");
  }

  // 2) Do/does/did also require the base form.
  const aux=/\\b(do|does|did)\\s+([a-z]+)\\b/gi;
  while((m=aux.exec(text))){
    const v=m[2].toLowerCase();
    if(v.endsWith("ies")) add(m[0],m[1]+" "+v.slice(0,-3)+"y","After do/does/did, use the base form of the verb.");
    else if(v.endsWith("es")) add(m[0],m[1]+" "+v.slice(0,-2),"After do/does/did, use the base form of the verb.");
    else if(v.endsWith("s")) add(m[0],m[1]+" "+v.slice(0,-1),"After do/does/did, use the base form of the verb.");
  }

  // 3) Subject-verb agreement for common pronoun subjects.
  const agreement=[
    [/(^|\\s)(i)\\s+(is|are|has)\\b/i,"I is","I am","Use am with I."],
    [/(^|\\s)(you|we|they)\\s+(is|was|has)\\b/i,null,null,"Use are/were/have with you, we and they."],
    [/(^|\\s)(he|she|it)\\s+(are|were|have)\\b/i,null,null,"Use is/was/has with he, she and it."]
  ];
  agreement.forEach(([re,bad,good,reason])=>{
    const x=text.match(re);
    if(!x)return;
    const subject=x[2].toLowerCase(),verb=x[3].toLowerCase();
    const map={
      i:{is:"I am",are:"I am",has:"I have"},
      you:{is:"you are",was:"you were",has:"you have"},
      we:{is:"we are",was:"we were",has:"we have"},
      they:{is:"they are",was:"they were",has:"they have"},
      he:{are:"he is",were:"he was",have:"he has"},
      she:{are:"she is",were:"she was",have:"she has"},
      it:{are:"it is",were:"it was",have:"it has"}
    };
    add(subject+" "+verb,map[subject][verb],reason);
  });

  // 3b) Missing copula after subject pronouns: "she my wife" -> "she is my wife".
  const copulaMissing=text.match(/\\b(i|you|he|she|it|we|they)\\s+(my|your|his|her|our|their)\\s+([a-z]+)\\b/i);
  if(copulaMissing){
    const subject=copulaMissing[1].toLowerCase();
    const verb=subject==="i"?"am":(subject==="you"||subject==="we"||subject==="they")?"are":"is";
    add(copulaMissing[0],subject+" "+verb+" "+copulaMissing[2]+" "+copulaMissing[3],"A subject pronoun needs a form of be before a possessive phrase here.");
  }

  // 4) Common singular/plural noun agreement without naming individual sentences.
  if(/\\b(people|children|men|women|cars|books|things|students|friends)\\s+is\\b/i.test(text))
    add(text.match(/\\b(people|children|men|women|cars|books|things|students|friends)\\s+is\\b/i)[0],
        text.match(/\\b(people|children|men|women|cars|books|things|students|friends)\\s+is\\b/i)[0].replace(/\\bis\\b/i,"are"),
        "Plural subjects normally take are.");
  if(/\\b(people|children|men|women|cars|books|things|students|friends)\\s+was\\b/i.test(text))
    add(text.match(/\\b(people|children|men|women|cars|books|things|students|friends)\\s+was\\b/i)[0],
        text.match(/\\b(people|children|men|women|cars|books|things|students|friends)\\s+was\\b/i)[0].replace(/\\bwas\\b/i,"were"),
        "Plural subjects normally take were.");

  // 5) Singular third-person simple present: he/she/it + base verb.
  const third=/\\b(he|she|it)\\s+([a-z]+)\\b/gi;
  while((m=third.exec(text))){
    const v=m[2].toLowerCase();
    const ignored=["is","was","has","does","can","could","may","might","must","should","will","would","to","not","very","more","less","really","never","always"];
    if(ignored.includes(v)) continue;
    if(/^[a-z]+$/.test(v) && !/(s|ed|ing)$/.test(v)){
      let thirdForm=v;
      if(/(s|x|z|ch|sh|o)$/.test(v)) thirdForm=v+"es";
      else if(/[^aeiou]y$/.test(v)) thirdForm=v.slice(0,-1)+"ies";
      else thirdForm=v+"s";
      add(m[0],m[1]+" "+thirdForm,"In the simple present, he/she/it usually takes the third-person singular verb form.");
    }
  }

  // 6) Perfect constructions: have/has + past participle. Catch common -ed/-en forms generically.
  if(/\\b(has|have|had)\\s+went\\b/i.test(text))
    add(text.match(/\\b(has|have|had)\\s+went\\b/i)[0],text.match(/\\b(has|have|had)\\s+went\\b/i)[1]+" gone","Use the past participle after have/has/had.");

  // 7) Double comparatives/superlatives.
  const doubleComp=text.match(/\\bmore\\s+(better|worse|faster|slower|bigger|smaller|stronger|weaker|higher|lower|easier|harder|older|younger|closer|farther)\\b/i);
  if(doubleComp) add(doubleComp[0],doubleComp[1],"Do not use more with an adjective that already has a comparative form.");
  const doubleSuper=text.match(/\\bmost\\s+(best|worst|fastest|slowest|biggest|smallest|strongest|weakest|highest|lowest|easiest|hardest|oldest|youngest)\\b/i);
  if(doubleSuper) add(doubleSuper[0],doubleSuper[1],"Do not use most with an adjective that already has a superlative form.");

  // 8) Infinitive patterns.
  const infinitive=text.match(/\\b(want|need|like|plan|hope|try|decide|learn)\\s+([a-z]+)\\b/i);
  if(infinitive && !/^(to|is|are|was|were|am|have|has|had)$/i.test(infinitive[2]))
    add(infinitive[0],infinitive[1]+" to "+infinitive[2],"These verbs commonly take to + the base verb.");

  // 9) Articles before singular countable nouns (small, conservative vocabulary).
  const article=text.match(/\\b(a|an)\\s+(apple|orange|hour|honest|university|unicorn|useful|European)\\b/i);
  if(article){
    const noun=article[2].toLowerCase();
    const wantsAn=/^(apple|orange|hour|honest)$/.test(noun);
    const good=wantsAn?"an":"a";
    if(article[1].toLowerCase()!==good) add(article[0],good+" "+noun,"Choose a/an based on the sound at the start of the next word.");
  }

  // 10) There is/are with plural nouns.
  const there=text.match(/\\bthere\\s+(is|was)\\s+(?:a\\s+)?(people|things|cars|books|students|friends)\\b/i);
  if(there) add(there[0],"there "+(there[1].toLowerCase()==="is"?"are":"were")+" "+there[2],"Use the plural form with a plural noun.");

  // 11) Common negative agreement.
  const neg=text.match(/\\b(he|she|it)\\s+(don't|do not)\\b/i);
  if(neg) add(neg[0],neg[1]+" doesn't","Use doesn't with he, she and it.");

  // 12) Remove 'to' after a modal.
  const modalTo=text.match(/\\b(can|could|may|might|must|should|will|would)\\s+to\\s+([a-z]+)\\b/i);
  if(modalTo) add(modalTo[0],modalTo[1]+" "+modalTo[2],"A modal verb is followed directly by the base verb.");

  return fixes;
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
  if(!normalized || normalized.split(/\\s+/).filter(Boolean).length<3){
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
  const text=normalizeSpeechText(raw);
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
  };

  rec.onerror=e=>{
    $("grammarStatus").textContent="Could not hear you ("+e.error+"). You can try again.";
    btn.disabled=false;
    btn.textContent="🎤 Speak a sentence";
    send.disabled=grammarPendingText.split(/\s+/).filter(Boolean).length<2;
  };

  rec.onend=()=>{
    if(btn.disabled && grammarPendingText.trim()){
      btn.disabled=false;
      btn.textContent="🎤 Speak a sentence";
      $("grammarStatus").textContent="Speech captured. Review it above, then tap Check sentence.";
      send.disabled=grammarPendingText.split(/\s+/).filter(Boolean).length<2;
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

function harperResult(text,lints){
  let corrected=text;
  const findings=[];
  [...lints].sort((a,b)=>b.span().start-a.span().start).forEach(lint=>{
    const span=lint.span();
    const suggestions=lint.suggestions();
    const suggestion=suggestions&&suggestions.length?suggestions[0]:null;
    const wrong=text.slice(span.start,span.end);
    const good=suggestion?suggestion.get_replacement_text():"";
    findings.push({wrong,good,message:lint.message()});
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
  if(!result.text || result.text.split(/\\s+/).length<2){
    box.className="feedback bad";
    box.innerHTML="⚠️ Please say a complete sentence so I can check it.";
    return;
  }

  box.className="feedback";
  box.innerHTML="🔎 Checking grammar...";
  $("grammarStatus").textContent="Checking your sentence locally...";

  const h=await harperCheckSentence(result.text);
  const localFixes=result.fixes;
  let findings=[];
  let corrected=result.text;
  let engineLabel="Systematic local grammar engine";

  if(h.available){
    const checked=harperResult(result.text,h.lints);
    corrected=checked.corrected;
    findings=checked.findings.map(x=>({wrong:x.wrong,good:x.good,message:x.message,source:"Harper"}));
    engineLabel="Harper + systematic grammar engine";
  }

  // Always merge the systematic grammar engine with Harper.
  // This prevents a broad grammar category from being missed just because Harper
  // did not flag that particular construction.
  localFixes.forEach(x=>{
    const duplicate=findings.some(f=>f.wrong.toLowerCase()===x.bad.toLowerCase() && f.good.toLowerCase()===x.good.toLowerCase());
    if(!duplicate) findings.push({wrong:x.bad,good:x.good,message:x.reason,source:"Grammar engine"});
    if(x.bad && x.good && x.bad!==x.good){
      const escaped=x.bad.replace(/[.*+?^()$|[\\]\\]/g,"\\\\$&");
      corrected=corrected.replace(new RegExp(escaped,"i"),x.good);
    }
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
    box.innerHTML="<div class='grammar-title'>✅ Looks good</div><div class='correction'>"+result.text+"</div><div class='explanation'>No issue was detected by "+engineLabel+".</div>";
    $("grammarStatus").textContent="No issue detected.";
  }
}
$("grammarSpeak").onclick=startGrammarTool;
$("grammarSend").onclick=()=>{
  if(!grammarPendingText.trim())return;
  try{if(grammarRecognition)grammarRecognition.stop()}catch(e){}
  const text=grammarPendingText.trim();
  $("grammarStatus").textContent="Checking your sentence...";
  showGrammarToolResult(text);
  $("grammarSend").disabled=true;
};
render();