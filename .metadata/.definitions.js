var questionsChosen = [];
var firstQuestion = "SET_IN_READY";
var lastQuestion = "SET_IN_READY";
var dropdownThreshold = 7;

var getNextQuestion = function (oid) {
    if (branchTable.hasOwnProperty(oid))
        return $("#"+branchTable[oid]);
    return $("#"+oid).closest("div").next();
};

var showNextQuestion = function (oid) {
    var currQ = $("#"+oid).closest("div");
    var nextQ = getNextQuestion(oid);
    $('div').hide(); //this is a hack
    if (questionsChosen.lastIndexOf(currQ.attr("id"))===-1)
        questionsChosen.push(currQ.attr("id"));
    displayQ(nextQ.attr("id"));
    nextQ.show();
};

var showPrevQuestion = function (currentQuid) {
    $("#"+currentQuid).hide();
    $("#"+questionsChosen.pop()).show();
};

var showNext = function(quid, oid) {
    if (lastQuestion.id!==quid) {
        $("#next_"+quid).click(function () {
            showNextQuestion(oid);
        });
        $("#next_"+quid).show();
    }
    $("#submit_"+quid).show();
};

var notAlreadyDisplayed = function (quid) {
    return $("#"+quid+" p input").length === 0 && $("#"+quid+" p select").length === 0;
};

var displayQ = function (quid) {
    var appendString = "";
    var i = 0;
    var text = "";
    var oid = "";
    if (notAlreadyDisplayed(quid)) {
        var inputType = oTable[quid]["input"];
        var data = oTable[quid]["data"];
        if (data.length > dropdownThreshold) {
            appendString = appendString
                            + "<select "+ ((inputType==="checkbox")?"multiple ":"")
                            + "id='select_"+quid
                            + "' onchange='showNext(\""+quid+"\", getDropdownOpt(\""+quid+"\"))'>"
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
                    + "<input type='"+inputType
                    +"' name='"+quid
                    +"' value='"+value+";"+questionsChosen.length+";"+i
                    +"' id='"+oid
                    +"' onclick='showNext(\""+quid+"\", \""+oid+"\")' />"
                    +"<label for='"+oid+"'>"+text+"</label>";
            }
        }
        $("#"+quid+" p").append(appendString);
    }
};

var getDropdownOpt = function(quid) {
    return $("#select_"+quid+" option:selected").val().split(";")[0];
};


