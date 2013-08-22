var questionsChosen = [];
var firstQuestion = "SET_IN_READY";
var lastQuestion = "SET_IN_READY";

var getNextQuestionID = function (oid) {
    if (branchTable.hasOwnProperty(oid))
        return $("#"+branchTable[oid]);
    return $("#"+oid).closest("div").next();
};

var showNextQuestion = function (oid) {
    var currQ = $("#"+oid).closest("div");
    var nextQ = getNextQuestionID(oid);
    $('div').hide(); //this is a hack
    if (questionsChosen.lastIndexOf(currQ.attr("id"))==-1)
        questionsChosen.push(currQ.attr("id"));
    nextQ.show();
};

var showPrevQuestion = function (currentQuid) {
    $("#"+currentQuid).hide();
    $("#"+questionsChosen.pop()).show();
};

var showNext = function(quid, oid) {
    if (lastQuestion.id!=quid) {
        $("#next_"+quid).click(function () {
            showNextQuestion(oid);
        });
        $("#next_"+quid).show();
    }
    $("#submit_"+quid).show();
};

var getDropdownOpt = function(quid) {
    return $("#select_"+quid+" option:selected").val();
};

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
    $("#preview").hide();
    questions.hide();
    if (assignmentId=="ASSIGNMENT_ID_NOT_AVAILABLE") {
        $("#preview").show();
    } else {
        $(firstQuestion).show();
    }
});
