var questionsChosen = [];
var firstQuestionId = "SET_IN_JS_GENERATOR"; //question id string
var dropdownThreshold = 7;
var id = 0;
var qTransTable = {};
var qTable = {};
var oTable = {};
var bList = [];

var getNextID = function() {
    id += 1;
    return "ans"+id;
};

var showFirstQuestion = function() {
    showQuestion(firstQuestionId);
    showOptions(firstQuestionId);
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

var showSubmit = function(quid, oid) {
    for (var i = 0 ; i < bList.length ; i++) {
        if (bList[i] === quid)
            return true;
    }
    return false;
};

var getNextQuestion = function (quid, oid) {
    if (branchTable.hasOwnProperty(oid)) {
        qTable[quid] = branchTable[oid];
    }
    return qTable[quid];
};

var registerAnswerAndShowNextQuestion = function (pid, quid, oid) {
    $("form").append($("#"+pid));
    $("#"+pid).hide();
    questionsChosen.push(quid);
    var nextQ = getNextQuestion(oid);
    showQuestion(nextQ.id);
    showOptions(nextQ.id);
};


var showNextButton = function(pid, quid, oid) {
    var nextHTML = "<input id=\"next_"+quid+"\" type=\"button\" value=\"Next\""
            + "onclick=\"registerAnswerAndShowNextQuestion('"
            + pid+"', '"
            + quid+"', '"
            + oid+"')\ />";
    var submitHTML = "";
    if (showSubmit(quid, oid))
        submitHTML = "<input id=\"submit_"+quid+"\" type=\"submit\" value=\"Submit\" />"
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
    if (data.length > dropdownThreshold) {
        appendString = appendString
                        + "<select "+ ((inputType==="checkbox")?"multiple ":"")
                        + "id='select_"+quid
                        + "' onchange='showNextButton(\""+pid+"\", \""+quid+"\", getDropdownOpt(\""+quid+"\"))'>"
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