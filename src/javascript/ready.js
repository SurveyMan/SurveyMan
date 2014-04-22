$(document).ready(function() {
    // assume there exists a sm variable
    //var assignmentId = "";
    if (navigator.onLine && !localp) {
        assignmentId = turkGetParam('assignmentId', "");
    } else if (localp) {
        //turkSetAssignmentID();
        aid = document.getElementById('assignmentId').value;
    } else {
        assignmentId = "ASSIGNMENT_ID_NOT_AVAILABLE";
    }

    $('form').submit(function() {
        window.onbeforeunload = null;
    });

    console.log(assignmentId);

    if ( assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $("#preview").show();
    } else {
        $("#preview").hide();
        aid = document.getElementById('assignmentId').value;
        console.log("assignmentId: " + assignmentId);
        Math.seedrandom(assignmentId);
        console.log(Math.random());
        sm = SurveyMan(jsonizedSurvey);
        sm.randomize();
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