var assignmentId = "OFFLINE";
var turkGetParam = function(a, b) { return assignmentId; };
var turkSetAssignmentID = function() { return; };
var offlineFormAction = function() { return; };
var clearBody = function () {
                    $("body").remove();
                    $("body").append("Thanks!");
                };

document.getElementById('mturk_form').action = "thanks.html"