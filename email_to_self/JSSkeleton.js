var counter = 0;

var updateCounter = function() {
    $("#counter").html("Answered "+ counter+" questions out of "+$("div[name=question]").length);
    $("#counter").show();
};

$("input:submit").hide()

$("#submit_"+lastQuestion.id).show()

$("#custom_check").hide()

//updateCounter();

showNext = function(quid, oid) {
    if (lastQuestion.id!==quid) {
        $("#next_"+quid).click(function () {
            showNextQuestion(oid);
        });
        $("#next_"+quid).show();
//	updateCounter();
//	    counter++;

    } else {
	$("#submit_"+quid).show();
	$("#custom_check").show();
    }
};
