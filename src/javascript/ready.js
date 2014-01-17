$(document).ready(function() {
    // assume there exists a sm variable
    assignmentId = turkGetParam('assignmentId', "");

    $('form').submit(function() {
        window.onbeforeunload = null;
    });

    if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $("#preview").show();
    } else {
        $("#preview").hide();
        if (sm.survey.breakoff) {
            sm.showBreakoffNotice();
        } else {
            sm.showFirstQuestion();
        }
    }
    if (customInit) {
        customInit();
    }
});