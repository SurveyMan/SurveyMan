$(document).ready(function() {
    assignmentId = turkGetParam('assignmentId', "");
    var count = 1;
    $('#preview').hide();
    $("[name='commit']").hide();
    $("[name='question']").addClass('questionDiv').hide();
    if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $('#preview').show();
    }
    else {
        if ($('.questionDiv').length == 1) {
            $("[name = 'commit']").show();
            $("[name = 'next']").hide();
        }
        $('.questionDiv:first').show();
    }
    $('input[name="next"]').click(function(){
        if($(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0) {
            count = count + 1;
            if (count == $('.questionDiv').length) {
                $("[name='next']").hide();
                $("[name='commit']").show();
            }
            $(this).parents('.questionDiv').hide();
            $(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();
        }
    });
    $('input[name="prev"]').click(function(){
        if($(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).length > 0) {
            count = count - 1;
            $("[name='next']").show();
            $("[name='commit']").hide();
            $(this).parents('.questionDiv').hide();
            $(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).show();
        }
    });
    var warning = !(assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE");
    window.onbeforeunload = function() {
        if(warning) {
            return "You have made changes on this page that you have not yet confirmed. If you navigate away from this page you will lose your unsaved changes";
        }
    }
    $('form').submit(function() {
        window.onbeforeunload = null;
    });

});

var moreQuestions = function (quid) {
    // checks whether there are more questions after my current question
    var questions = $("[name='question'");
    var lastQuestion = questions[questions.length-1];
    return lastQuestion.id!=quid;
}

var showNext = function(id) {
    var nextid = "#next_"+id;
    var submitid = "#submit_"+id;
    //if (moreQuestions(id))
    $(nextid).show();
    $(submitid).show();
};