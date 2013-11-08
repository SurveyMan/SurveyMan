$(document).ready(function() {
    assignmentId = turkGetParam('assignmentId', "");

    $('form').submit(function() {
        window.onbeforeunload = null;
    });

    if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $("#preview").show();
    } else {
        $("#preview").hide();
        if (bList.length > 0)
            showBreakoffNotice();
        else showFirstQuestion();
    }
    if (customInit) {
        customInit();
    }
});