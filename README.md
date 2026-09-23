# English AI Server + Browser Speaking Practice

The repository now contains a serverless browser speaking-practice website.

Features:
- Browser Text-to-Speech
- Browser Speech Recognition where supported
- Real-world speaking scenarios
- Sentence similarity feedback
- Common grammar feedback
- Retry and repeat practice
- 60-second free-speaking challenge

The speaking page is index.html with style.css and app.js. It does not call AWS.

Hosting:
- GitHub Pages can publish the static files.
- Firebase Hosting can publish the same static site on a web.app or firebaseapp.com domain.

The current grammar feedback is intentionally lightweight and runs in the browser. It is not a full LanguageTool replacement yet. This keeps the first version serverless and free.