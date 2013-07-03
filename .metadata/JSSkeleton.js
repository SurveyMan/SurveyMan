$(document).ready(function() {
    assignmentId = turkGetParam('assignmentId', "");
    var count = 0;
    $('#preview').hide();
    $("[name='submit']").hide();
    $('div[id^="question"]').addClass('questionDiv').hide();
    if (assignmentId==="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $('#preview').show();
    }
    else {
        $('.questionDiv:first').show();
    }
    $('input[name="next"]').click(function(){
        if($(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0) {
            count = count + 1;
            if (count === $('.questionDiv').length - 1) {
                $("[name='next']").hide();
                $("[name='submit']").show();
            }
            $(this).parents('.questionDiv').hide();
            $(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();
        }
    });
    $('input[name="prev"]').click(function(){
        if($(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).length > 0) {
            count = count - 1;
            $("[name='next']").show();
            $("[name='submit']").hide();
            $(this).parents('.questionDiv').hide();
            $(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).show();
        }
    });
    var warning = !(assignmentId==="ASSIGNMENT_ID_NOT_AVAILABLE");
    window.onbeforeunload = function() {
        if(warning) {
            return "You have made changes on this page that you have not yet confirmed. If you navigate away from this page you will lose your unsaved changes";
        }
    };
    $('form').submit(function() {
        window.onbeforeunload = null;
    });
});
