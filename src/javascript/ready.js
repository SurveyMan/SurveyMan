$(document).ready(function() {
    // assume there exists a sm variable
    if (navigator.onLine && !localp) {
        assignmentId = turkGetParam('assignmentId', "");
    } else {
        assignmentId = "OFFLINE";
    }

    $('form').submit(function() {
        window.onbeforeunload = null;
    });

    console.log(assignmentId);

    if ( assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $("#preview").show();
    } else {
        $("#preview").hide();
        Math.seedrandom(assignmentId);
        sm = SurveyMan(jsonizedSurvey);
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