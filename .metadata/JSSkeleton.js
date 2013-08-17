var questions = "QUESTIONS";
var lastQuestion = "LASTQUESTION";
var firstQuestion = "FIRSTQUESTION";
var currentQ = 0;

$(document).ready(function() {
    assignmentId = turkGetParam('assignmentId', "");

    $('input[name="next"]').click(function(){
        getNextQuestion();
    });
    $('input[name="prev"]').click(function(){
        getPrevQuestion();
    });
    window.onbeforeunload = function() {
        var warning = !(assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE");
        if(warning) {
            return "You have made changes on this page that you have not yet confirmed. If you navigate away from this page you will lose your unsaved changes";
        }
    }
    $('form').submit(function() {
        window.onbeforeunload = null;
    });

    initialize();
});

function initialize() {
	currentQ = 0;
    questions = $('[name="question"]');
    lastQuestion = questions[questions.length-1];
	firstQuestion = questions[0];
	hideFirstPrev();
    hideDiv("#preview");
    questions.hide();
    if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        showDiv("#preview");
    }
    showDiv(firstQuestion);
}

function hideFirstPrev() {
	$(firstQuestion).find("[name='prev']").hide();
}

function isFirstQuestion(id) {
    return id == firstQuestion.id;    
}

function isLastQuestion(id) {
    return id == lastQuestion.id;
}

function hideDiv(div) {
    $(div).hide();
}

function showDiv(div) {
	$(div).show();
}

function getNextQuestion() {
    if(!isLastQuestion(questions[currentQ].id)) {
        if (isLastQuestion(questions[currentQ+1].id)) {
            $("[name='next']").hide();
        }
        hideDiv(questions[currentQ]);
        showDiv(questions[currentQ+1]);
		currentQ = currentQ + 1;
    }
}

function getPrevQuestion() {
    if(!isFirstQuestion(questions[currentQ].id)) {
        $("[name='next']").show();
        hideDiv(questions[currentQ]);
        showDiv(questions[currentQ-1]);
		currentQ = currentQ - 1;
    }
}

var showNext = function(id) {
    var nextid = "#next_"+id;
    var submitid = "#submit_"+id;
    if (!isLastQuestion(id)) {
        $(nextid).show();
    }
    $(submitid).show();
};