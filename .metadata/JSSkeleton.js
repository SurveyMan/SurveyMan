var questions = "QUESTIONS";
var lastQuestion = "LASTQUESTION";

$(document).ready(function() {
    var currentQ = 1;
    assignmentId = turkGetParam('assignmentId', "");
    function initialize() {
        $('#preview').hide();
        $("[name='prev']").hide();
        $("[name='next']").hide();
        $("[name='commit']").hide();
        $("[name='question']").addClass('questionDiv').hide();
        if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
            $('#preview').show();
        }
            $('.questionDiv:first').show();
    }
    function isFirstQuestion() {
        return currentQ == 1;
    }
    function isLastQuestion() {
        return currentQ == $('.questionDiv').length;
    }
    function showNextQuestion(button) {
        if($(button).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0) {
            currentQ = currentQ + 1;
            $("[name='prev']").show();
            $(button).parents('.questionDiv').hide();
            $(button).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();
        }
    }
    function showPrevQuestion(button) {
        if($(button).parents('.questionDiv').prevAll('.questionDiv').eq(0).length > 0) {
            currentQ = currentQ - 1;
            if (isFirstQuestion()) {
                $("[name='prev']").hide();
            }
            $(button).parents('.questionDiv').hide();
            $(button).parents('.questionDiv').prevAll('.questionDiv').eq(0).show();
        }
    }
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
    questions = $('[name="question"]');
    lastQuestion = questions[questions.length-1];
    initialize();
});

var showNext = function(id) {
    var nextid = "#next_"+id;
    var submitid = "#submit_"+id;
    if (lastQuestion.id!=id) {
        $(nextid).show();
    }
    $(submitid).show();
};
