var assignmentId = "OFFLINE";
var turkGetParam = function(a, b) { return assignmentId; };
var turkSetAssignmentID = function() { return; };
var offlineFormAction = function() { return; };

document.getElementById('mturk_form').action = offlineFormAction;