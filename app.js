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
  return t.replace(/\\s+/g," ").trim();
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
  const rules=[
    [/\b(more)\s+(better|worse|faster|slower|bigger|smaller|stronger|weaker|higher|lower|easier|harder|older|younger|closer|farther)\b/i,"more $2","$2"],
    [/\b(i)\s+is\b/i,"I is","I am"],
    [/\b(i)\s+are\b/i,"I are","I am"],
    [/\b(i)\s+has\b/i,"I has","I have"],
    [/\b(he|she|it)\s+go\b/i,"go","goes"],
    [/\b(he|she|it)\s+have\b/i,"have","has"],
    [/\b(he|she|it)\s+do\b/i,"do","does"],
    [/\b(he|she|it)\s+don't\b/i,"don't","doesn't"],
    [/\b(i|you|we|they)\s+is\b/i,"is","are"],
    [/\b(i|you|we|they)\s+was\b/i,"was","were"],
    [/\b(i|you|we|they)\s+has\b/i,"has","have"],
    [/\b(he|she|it|my father|my mother|my brother|my sister|the man|the woman|the boy|the girl)\s+were\b/i,"were","was"],
    [/\b(he|she|it|my father|my mother|my brother|my sister|the man|the woman|the boy|the girl)\s+are\b/i,"are","is"],
    [/\b(he|she|it)\s+don't\b/i,"don't","doesn't"],
    [/\b(yesterday|last night|last week|last month)\s+[^.]*\b(go|eat|buy|see|come)\b/i,"past-tense verb","use the past tense"],
    [/\b(can|should|must)\s+to\s+/i,"to + verb","can/should/must + verb"],
    [/\b(want|need|like)\s+go\b/i,"want/need/like go","want/need/like to go"],
    [/\b(people|children|men|women)\s+is\b/i,"is","are"],
    [/\b(my father|my mother|my brother|my sister|the man|the woman|the boy|the girl)\s+was\b/i,"was","was"],
    [/\b(my father|my mother|my brother|my sister|the man|the woman|the boy|the girl)\s+have\b/i,"have","has"],
    [/\b(there)\s+is\s+[^.]*\b(people|things|cars|books)\b/i,"there is","there are"]
  ];
  return rules.filter(r=>r[0].test(text)).map(r=>({bad:r[1],good:r[2]}));
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

async function harperCheckSentence(text){
  if(!window.harperLinter){
    return {available:false,lints:[]};
  }
  try{
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
    if(suggestion){
      corrected=corrected.slice(0,span.start)+good+corrected.slice(span.end);
    }
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
  $("grammarStatus").textContent="Harper is checking your sentence locally...";

  const h=await harperCheckSentence(result.text);

  if(h.available){
    const checked=harperResult(result.text,h.lints);
    grammarLastCorrection=checked.corrected;
    $("grammarListen").disabled=false;

    if(checked.findings.length){
      box.className="feedback bad";
      box.innerHTML="<div class='grammar-title'>❌ Mistake found</div>"+
        "<div class='correction'><b>Better sentence:</b> "+checked.corrected+"</div>"+
        checked.findings.map(x=>"<div>❌ <strong>"+x.wrong+"</strong> → <strong>"+(x.good||"remove")+"</strong><br><small>"+x.message+"</small></div>").join("<br>")+
        "<div class='explanation'>Checked locally by Harper. Your sentence is not sent to a grammar server.</div>";
      $("grammarStatus").textContent="Harper found grammar feedback.";
    }else{
      box.className="feedback good";
      box.innerHTML="<div class='grammar-title'>✅ Looks good</div><div class='correction'>"+result.text+"</div><div class='explanation'>Harper did not detect a grammar or spelling issue.</div>";
      $("grammarStatus").textContent="Harper found no issue in this sentence.";
    }
    return;
  }

  // Local fallback if Harper cannot load.
  grammarLastCorrection=result.corrected;
  $("grammarListen").disabled=false;
  if(result.fixes.length){
    box.className="feedback bad";
    box.innerHTML="<div class='grammar-title'>❌ Mistake found</div><div class='correction'><b>Better sentence:</b> "+result.corrected+"</div>"+
      result.fixes.map(x=>"<div>❌ <strong>"+x.bad+"</strong> → <strong>"+x.good+"</strong></div>").join("")+
      "<div class='explanation'>Harper could not load, so the local fallback rules were used.</div>";
    $("grammarStatus").textContent="Harper unavailable. Local grammar rules were used.";
  }else{
    box.className="feedback good";
    box.innerHTML="<div class='grammar-title'>ℹ️ Basic check</div><div class='correction'>"+result.text+"</div><div class='explanation'>Harper could not load, so only the local fallback rules were used.</div>";
    $("grammarStatus").textContent="Harper unavailable. Basic local check completed.";
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