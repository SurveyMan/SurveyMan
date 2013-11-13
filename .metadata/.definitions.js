var questionsChosen = [];
var firstQuestionId = "SET_IN_JS_GENERATOR"; //question id string
var lastQuestionId = "SET_IN_JS_GENERATOR";
var dropdownThreshold = 7;
var id = 0;
var qTransTable = {};
var qTable = {};
var oTable = {};
var bList = [];
var oneBranchTable = {};

var getNextID = function() {
    id += 1;
    return "ans"+id;
};

var showBreakoffNotice = function() {
    $(".question").append("<p> A button will appear momentarily to continue the survey. In the meantime, please read:</p><p>This survey will allow you to submit partial responses. The minimum payment is the quantity listed. However, you will be compensated more for completing more of the survey in the form of bonuses. The quantity paid depends on the results returned so far. Note that submitting partial results does not guarantee payment.</p>");
       $("div[name=question]").show();
    setTimeout(function () {
                    $(".question").append("<input type=\"button\" value=\"Continue\" onclick=\"showFirstQuestion()\" />");
                }
                , 5000);
};

var showFirstQuestion = function() {
    showQuestion(firstQuestionId);
    showOptions(firstQuestionId);
    $("div[name=question]").show();
};

var showQuestion = function (quid) {
    var questionHTML = qTable[quid];
    $(".question").empty();
    $(".question").append(questionHTML);
};

var showOptions = function(quid) {
    var optionHTML = getOptionHTML(quid);
    $(".answer").empty();
    $(".answer").append(optionHTML);
};

var showEarlySubmit = function(quid, oid) {
    for (var i = 0 ; i < bList.length ; i++) {
        if (bList[i] === quid)
            return true;
    }
    return false;
};

var getNextQuestion = function (quid, oid) {
    if (branchTable.hasOwnProperty(oid)) {
        if (oneBranchTable.hasOwnProperty(quid)) {
            // update this block's last question to transition to the appropriate place
            qTransTable[oneBranchTable[quid]] = branchTable[oid];
        } else {
            // we're a branch all
            qTransTable[quid] = branchTable[oid];
        }
    }
    return qTransTable[quid];
};

var registerAnswerAndShowNextQuestion = function (pid, quid, oid) {
    $("form").append($("#"+pid));
    $("#"+pid).hide();
    questionsChosen.push(quid);
    var nextQuid = getNextQuestion(quid, oid);
    showQuestion(nextQuid);
    showOptions(nextQuid);
    $("#next_"+quid).remove();
    $("#submit_"+quid).remove();
};

var containsDropdown = function (pid) {
    return $("#"+pid+" select").length === 1;
};

var submitNotYetShown = function() {
    return $(":submit").length===0;
};

var showNextButton = function(pid, quid, oid) {
    var id = "next_"+quid;
    if ($("#"+id).length > 0)
        $("#"+id).remove();
    var nextHTML = "<input id=\""+id+"\" type=\"button\" value=\"Next\" "
            + " onclick=\"registerAnswerAndShowNextQuestion('"
            + pid+"', '"
            + quid+"', '"
            + oid+"')\" />";
    var submitHTML = "";
    if (quid===lastQuestionId && submitNotYetShown())
        submitHTML += "<input id=\"submit_"+quid+"\" type=\"submit\" value=\"Submit\" />";
    else if (showEarlySubmit(quid, oid) && submitNotYetShown())
        submitHTML += "<input id=\"submit_"+quid+"\" type=\"submit\" value=\"Submit Early\" class=\"breakoff\" />";
    if (quid!==lastQuestionId)
        $("div[name=question]").append(nextHTML);
    $("div[name=question]").append(submitHTML);
};

var getDropdownOpt = function(quid) {
    var dropdownOpt = $("#select_"+quid+" option:selected").val().split(";")[0];
    console.log("selected dropdown option: " + dropdownOpt);
    return dropdownOpt;
};

var getOptionHTML = function (quid) {
    var pid = getNextID();
    var appendString = "";
    var i = 0;
    var text = "";
    var oid = "";
    var inputType = oTable[quid]["input"];
    var data = oTable[quid]["data"];
    if (inputType === "text") {
        appendString = "<textarea form=\"mturk_form\" type=\"text\""
                        + " name=\""+quid+"\""
                        + " oninput='showNextButton(\""+pid+"\", \""+quid+"\", -1)'"
                        + " />";
    } else if (data.length > dropdownThreshold) {
        appendString = appendString
                        + "<select "+ ((inputType==="checkbox")?"multiple ":"") 
                        + " form=\"mturk_form\" "
                        + " id=\"select_"+quid
                        + "\" name=\""+quid
                        + "\" onchange='showNextButton(\""+pid+"\", \""+quid+"\", getDropdownOpt(\""+quid+"\"))'>"
                        + "<option disable selected>CHOOSE ONE</option>";
        for ( ; i < data.length ; i++) {
            text = data[i]["text"];
            oid = data[i]["value"];
            appendString = appendString
                           + "<option value='"+oid+";"+questionsChosen.length+";"+i
                           +"' id='"+oid
                           +"'>"+text
                           +"</option>";
        }
        appendString = appendString + "</select>";
      } else {
          for (i = 0 ; i < data.length ; i++) {
              text = data[i]["text"];
              oid = data[i]["value"];
              value = (inputType==="text")?"":oid;
              appendString = appendString
                  +"<label for='"+oid+"'>"
                  + "<input type='"+inputType
                  +"' name='"+quid
                  +"' value='"+value+";"+questionsChosen.length+";"+i
                  +"' id='"+oid
                  +"' onchange='showNextButton(\""+pid+"\", \""+quid+"\", \""+oid+"\")' />"
                  +text+"</label>";
          }
      }
      return "<p id=\""+pid+"\">"+appendString+"</p>";
};