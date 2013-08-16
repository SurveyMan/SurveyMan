var questions = "QUESTIONS";
var lastQuestion = "LASTQUESTION";
var currentQ = 1;
var canskip = false;

$(document).ready(function() {
    currentQ = 1;
    assignmentId = turkGetParam('assignmentId', "");

    $('input[name="next"]').click(function(){
        showNextQuestion(this);
    });
    $('input[name="prev"]').click(function(){
        showPrevQuestion(this);
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
    canskip = checkCanSkip();
    questions = $('[name="question"]');
    lastQuestion = questions[questions.length-1];
    $('#preview').hide();
    $("[name='prev']").hide();
    $("[name='question']").addClass('questionDiv').hide();
    if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $('#preview').show();
    }
    $('.questionDiv:first').show();
}

function checkCanSkip() {
    // There may be a better way to check this. Right now, if previous buttons are not hidden by default, canskip is true
    return !$("[name='prev']").is(":hidden");
}

function isFirstQuestion() {
    return currentQ == 1;    
}

function isLastQuestion() {
    return currentQ == $('.questionDiv').length;
}

function hasNextQuestion(button) {
    return $(button).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0;
}

function hideParentDiv(element) {
    $(element).parents('.questionDiv').hide();
}

function showNextDiv(element) {
    $(element).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();
}

function showPrevDiv(element) {
    $(element).parents('.questionDiv').prevAll('.questionDiv').eq(0).show();
}

function showNextQuestion(button) {
    if(hasNextQuestion(button)) {
        currentQ = currentQ + 1;
        if (canskip)
        {
            $("[name='prev']").show();
        }
        if (isLastQuestion()) {
            $("[name='next']").hide();
        }
        hideParentDiv(button);
        showNextDiv(button);
    }
}

function hasPrevQuestion(button) {
    return $(button).parents('.questionDiv').prevAll('.questionDiv').eq(0).length > 0;
}

function showPrevQuestion(button) {
    if(hasPrevQuestion(button)) {
        currentQ = currentQ - 1;
        $("[name='next']").show();
        if (isFirstQuestion()) {
            $("[name='prev']").hide();
        }
        hideParentDiv(button);
        showPrevDiv(button);
    }
}

var showNext = function(id) {
    var nextid = "#next_"+id;
    var submitid = "#submit_"+id;
    if (lastQuestion.id!=id) {
        $(nextid).show();
    }
    $(submitid).show();
};
