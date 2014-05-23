// don't show breakoff notice
sm.showBreakoffNotice = function () {
    sm.showFirstQuestion();
};
sm.showBreakoffNotice();

// add timing information
var oldShowQuestion = sm.showQuestion;
var oldRegisterAndShowNextQuestion = sm.registerAnswerAndShowNextQuestion ;
var addTimingInfo = function(q, tag) {
    var start    =    document.createElement('input'),
        date     =    new Date(),
        perf     =    window.performance && window.performance.now;

  start.type='text';
  start.id=tag+'_'+q.id;
  start.name=tag+'_'+q.id;
  start.form='mturk_form';
  start.hidden=true;
  start.defaultValue = perf ? performance.now() : date.getTime();
  console.log(tag + " " + q.id + " " + start.value);
  document.getElementById('mturk_form').appendChild(start);
};
sm.showQuestion = function (q) {
  addTimingInfo(q,'start');
  oldShowQuestion(q);
};
sm.registerAndShowNextQuestion = function (pid,q,o){
  addTimingInfo(q,'end');
  oldRegisterAndShowNextQuestion(pid,q,o);
};
