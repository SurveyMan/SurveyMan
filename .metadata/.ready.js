$(document).ready(function() {
    assignmentId = turkGetParam('assignmentId', "");

    $('form').submit(function() {
        window.onbeforeunload = null;
    });

    questions = $('[name="question"]');
    lastQuestion = questions[questions.length-1];
    firstQuestion = questions[0];

	$(firstQuestion).find("[id^='prev']").hide();
	$(lastQuestion).find("[id^='next']").hide();
	//loadPreview();
    //$("#preview").hide();
    questions.hide();
    if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $("#preview").show();
    } else {
        $("#preview").hide();
        $(firstQuestion).show();
        displayQ(firstQuestion.id);
    }
    if (customInit) {
        customInit();
    }
});