// generate json (maps from before)
// instead of setting the vars directly, just provide them as args to these functions
// and have the function set them

var SurveyMan = function (jsonSurvey) {

    var allQuestions        =   [],
        currentQuestions    =   [],
        topBlocks           =   [],
        questionsChosen     =   [],
        dropdownThreshold   =   7,
        getQuestionById     =   function (quid) {

                                    var i;
                                    for ( i = 0 ; i < this.allQuestions.length ; i++ ) {
                                        if ( this.allQuestions[i].id === quid ) {
                                            return this.allQuestions[i];
                                        }
                                    }
                                    throw "Question id " + quid + " not found in allQuestions";

                                },
        range               =   function (n) {

                                    var i, rList = [];
                                    for ( i = 0 ; i < n ; i++ ) {
                                        rList.push(i);
                                    }
                                    return rList;

                                },
        Block               =   function(jsonBlock) {

                                    var idStringToArray = function (_idString) {
                                        return _.map(_idString.split("."), function(s) { parseInt(s); });
                                    };

                                    this.id = jsonBlock.id;
                                    this.idArray = idStringToArray(jsonBlock.id);
                                    this.topLevelQuestions = Question.makeQuestions(jsonBlock.questions, this);
                                    this.subblocks = [];
                                    // may need to call a to boolean on jsonBlock.randomize
                                    this.randomizable = jsonBlock.randomize || Block.randomizeDefault;
                                    this.getAllBlockQuestions = function () {
                                        // either one question is a branch or all, and they're always out of the top level block.
                                        // put the current block's questions in a global stack that we can empty
                                        //  how to interleave top-level questions and blocks?
                                        //  get the total number of "slots" and assign indices
                                        var i, j = 0, k = 0,
                                            retval = [],
                                            indices = range(this.topLevelQuestions.length + this.subblocks.length),
                                            qindices = _.sample(indices, this.topLevelQuestions.length),
                                            bindices = _.difference(indices, qindices);
                                        for ( i = 0 ; i < indices.length ; i++ ) {
                                          // it happens that i == indices[i]
                                          if (_.contains(qindices, i)) {
                                            retval.push(this.questions[j]);
                                            j++;
                                          } else if (_.contains(bindices, i)) {
                                            retval.append(this.subblocks[k].getAllBlockQuestions);
                                            k++;
                                          } else throw "Neither qindices nor bindices contain index " + i;
                                        }
                                        return retval;
                                    };
                                    this.getQuestion = function(quid) {
                                        var i;
                                        for ( i = 0 ; i < this.topLevelQuestions.length ; i++ ) {
                                            if ( this.topLevelQuestions[i].id == quid ) {
                                                return this.topLevelQuestions[i];
                                            }
                                        }
                                        throw "Question with id " + quid + " not found in block " + this.id;
                                    };
                                    this.idComp = function(that) {
                                        // returns whether that follows (+1), precedes (-1), or is a sub-block (0) of this
                                        var i, j;
                                        for ( i = 0 ; i < this.idArray ; i++ ) {
                                            if ( i < that.idArray.length ) {
                                                if ( this.idArray[i] < that.idArray[i] ) {
                                                    return -1;
                                                } else if ( this.idArray[i] > that.idArray[i] ) {
                                                    return 1;
                                                }
                                            }
                                            return 0;
                                        }
                                    };
                                    this.randomize = function () {
                                        var i, j, newSBlocks = _.map(range(this.subblocks.length), -1);
                                        // randomize questions
                                        _.shuffle(this.topLevelQuestions);
                                        // randomize blocks
                                        var stationaryBlocks = _.filter(this.subblocks, function (b) { return b.randomizable; }),
                                            nonStationaryBlocks = _.filter(this.subblocks, function (b) { return ! b.randomizable; }),
                                            samp = _.sample(range(this.subblocks.length), nonStationaryBlocks.length);
                                        _.shuffle(nonStationaryBlocks);
                                        for ( i = 0 ; i < samp.length ; i++ ) {
                                            // pick the locations for where to put the non-stationary blocks
                                            newSBlocks[samp[i]] = nonStationaryBlock[i];
                                        }
                                        for ( i = 0, j = 0; i < newSBlocks.length ; i++ ) {
                                            if ( newSBlocks[i] == -1 ) {
                                                newSBlocks[i] = stationaryBlocks[j];
                                                j++;
                                            }
                                        }
                                        console.assert(j == stationaryBlocks.length - 1);
                                        this.subblocks = newSBlocks;
                                        for ( i = 0 ; i < this.subblocks.length ; i++) {
                                            this.subblocks.randomize();
                                        }
                                    };
                                    this.populate = function () {
                                        var i;
                                        if (_.isUndefined(jsonBlock.subblocks)){
                                            console.log("No subblocks in Block " + this.id);
                                            return;
                                        }
                                        for ( i = 0 ; i < jsonBlock.subblocks.length ; i++ ) {
                                            var b = new Block(jsonBlock.subblocks[i]);
                                            b.parent = this;
                                            this.subblocks.push(b);
                                            b.populate();
                                        }
                                    };
                                    this.isLast = function (q) {
                                        return questions[questions.length - 1] === q;
                                    };
                                    // assert that the sub-blocks have the appropriate ids
                                    console.assert(_.every(this.subBlocks, function(b) { return this.idComp(b) === 0 }));

                                },
        Option              =   function(jsonOption, _question) {

                                    this.id = jsonOption.id;
                                    this.otext = jsonOption.otext;
                                    this.question = _question;

                                },
        Question            =   function(jsonQuestion, _block) {


                                    var makeBranchMap   =   function (branchMap, _question) {
                                                                var i, bm = {};
                                                                // branchMap -> map from oid to quid
                                                                if (!_.isUndefined(branchMap)) {
                                                                    var keys = _.keys(branchMap);
                                                                    for ( i = 0 ; i < keys.length ; i++ ) {
                                                                        var o = _question.getOption(keys[i]),
                                                                            q = getQuestionById(branchMap[keys[i]]);
                                                                            b = q.block;
                                                                        bm[o] = b;
                                                                    }
                                                                }
                                                                return bm;
                                                            };

                                    this.block = _block;
                                    this.id = jsonQuestion.id;
                                    this.qtext = jsonQuestion.qtext;
                                    this.options = Option.makeOptions(jsonQuestion.options, this);
                                    this.branchMap = makeBranchMap(jsonQuestion.branchMap, this);
                                    // FIELDS MUST BE SENT OVER AS STRINGS
                                    this.randomizable = jsonQuestion.randomize || Survey.randomizeDefault;
                                    this.ordered = jsonQuestion.ordered || Survey.orderedDefault;
                                    this.exclusive = jsonQuestion.exclusive || Survey.exclusiveDefault;
                                    this.breakoff = jsonSurvey.breakoff || Survey.breakoffDefault;
                                    this.getOption = function (oid) {
                                        var i;
                                        for ( i = 0 ; i < options.length ; i++ ) {
                                            if ( options[i].id === oid ) {
                                                return options[i];
                                            }
                                        }
                                        throw "Option id " + oid + " not found in question " + this.id;
                                    };
                                    this.randomize = function () {
                                        var i;
                                        if (this.ordered) {
                                            if (Math.random() < 0.5) {
                                                options.reverse();
                                            }
                                        } else {
                                            _.shuffle(options);
                                        }
                                    };

                                },
        Survey              =   function (jsonSurvey) {

                                    var makeSurvey = function(jsonSurvey) {
                                        var i, blockList = [];
                                        for ( i = 0 ; i < jsonSurvey.length ; i++ ) {
                                            blockList[i] = new Block(jsonSurvey[i]).populate();
                                        }
                                    };

                                    this.filename = jsonSurvey.filename;
                                    this.topLevelBlocks = makeSurvey(jsonSurvey.survey);
                                    this.breakoff = jsonSurvey.breakoff;
                                    this.firstQuestion = this.topLevelBlocks[0].topLevelQuestions[0];
                                    this.randomize = function () {
                                        var i;
                                        for ( i = 0 ; i < this.topLevelBlocks.length ; i++ ) {
                                            // contents of the survey
                                            this.topLevelBlocks[i].randomize();
                                        }
                                        this.firstQuestion = this.topLevelBlocks[0].topLevelQuestions[0];
                                    };

                                    topBlocks = topLevelBlocks;

                                };

    Question.makeQuestions  =   function (jsonQuestions, enclosingBlock) {
                                     var i, qList = [];
                                     for ( i = 0 ; i < jsonQuestions.length ; i++ ) {
                                         var q = new Question(jsonQuestions[i], enclosingBlock);
                                         qList.push(q);
                                         allQuestions.push(q);
                                     }
                                     return qList;
                                 };
    Option.makeOptions      =   function (jsonOptions, enclosingQuestion) {
                                     if (_.isUndefined(jsonOptions)) {
                                        console.log("No options defined for " + enclosingQuestion.id + " (" + enclosingQuestion.qtext + ")");
                                        assert(jsonOptions.freetext);
                                     }
                                     var i, oList = [];
                                     for ( i = 0 ; i < jsonOptions.length ; i++ ){
                                         oList.push(new Option(jsonOptions[i], enclosingQuestion));
                                     }
                                     return oList;
                                 };
    Survey.exclusiveDefault =   true;
    Survey.orderedDefault   =   false;
    Survey.randomizeDefault =   true;
    Survey.freetextDefault  =   false;
    Survey.breakoffDefault  =   true;
    Block.randomizeDefault  =   false;

    this.survey = new Survey(jsonSurvey);
    this.showBreakoffNotice = function() {
        $(".question").append("<p> A button will appear momentarily to continue the survey. In the meantime, please read:</p><p>This survey will allow you to submit partial responses. The minimum payment is the quantity listed. However, you will be compensated more for completing more of the survey in the form of bonuses. The quantity paid depends on the results returned so far. Note that submitting partial results does not guarantee payment.</p>");
        $("div[name=question]").show();
        setTimeout(function () {
                        $(".question").append("<input type=\"button\" value=\"Continue\" onclick=\"sm.showFirstQuestion()\" />");
                        }
                    , 5000);
        };
    this.showFirstQuestion = function() {
        this.showQuestion(this.survey.firstQuestion);
        this.showOptions(this.survey.firstQuestion);
    };
    this.showQuestion =  function(q) {
        $(".question").empty();
        $(".question").append(q.qtext);
    };
    this.showOptions = function(q) {
        $(".answer").empty();
        $(".answer").append(_.map(q.options, function (o) { return o.otext; }));
    };
    this.showEarlySubmit = function (q, o) {
        return q.breakoff;
    };
    this.getNextQuestion = function (q, o) {
        var b;
        if (!_.isUndefined(q.branchMap[o])) {
            // returns a block
            b = q.branchMap[o];
            currentQuestions = b.getAllBlockQuestions();
            return currentQuestions.shift();
        } else {
            // get the next sequential question
            if (currentQuestions.empty()) {
                // should never be called on empty topBlocks
                b = topBlocks.shift();
                currentQuestions = b.getAllBlockQuestions();
            }
            return currentQuestions.shift();
        }
    };
    this.registerAnswerAndShowNextQuestion = function (pid, q, o) {
        var q;
        $("form").append($("#"+pid));
        $("#"+pid).hide();
        questionsChosen.push(q);
        q = getNextQuestion(q, o);
        showQuestion(q);
        showOptions(q);
        $("#next_"+q.quid).remove();
        $("#submit_"+q.quid).remove();
    };
    this.submitNotYetShown = function () {
        return $(":submit").length === 0;
    };
    this.showNextButton = function (pid, q, o) {
        var id, nextHTML, submitHTML;
        id = "next_"+q.quid;
        if ($("#"+id).length > 0){
            $("#"+id).remove();
    	    $("#submit_"+quid).remove();
        }
        nextHTML = "<input id=\""+id+"\" type=\"button\" value=\"Next\" "
                + " onclick=\"registerAnswerAndShowNextQuestion('"
                + pid + "', '"
                + q + "', '"
                + o + "')\" />";
        submitHTML = "";
        if ( currentQuestions.length === 0 && topBlocks.length === 0 && submitNotYetShown())
            submitHTML += "<input id=\"submit_"+quid+"\" type=\"submit\" value=\"Submit\" />";
        else if (showEarlySubmit(q, o) && submitNotYetShown())
            submitHTML += "<input id=\"submit_"+quid+"\" type=\"submit\" value=\"Submit Early\" class=\"breakoff\" />";
        if ( currentQuestions.length === 0 && topBlocks.length === 0 )
            $("div[name=question]").append(nextHTML);
        $("div[name=question]").append(submitHTML);
    };
    this.getDropdownOpt = function(q) {
        var dropdownOpt = $("#select_" + q.quid + " option:selected").val().split(";")[0];
        console.log("selected dropdown option: " + dropdownOpt);
        return dropdownOpt;
    };
    this.getOptionHTML = function (q) {
        var o, i
            pid             =   getNextID(),
            appendString    =   "";
        if ( q.freetext ) {
            appendString = "<textarea form=\"mturk_form\" type=\"text\""
                            + " name=\""+quid+"\""
                            + " oninput='showNextButton(\""+pid+"\", \""+quid+"\", -1)'"
                            + " />";
        } else if ( q.options.length > dropdownThreshold ) {
            appendString = appendString
                           + "<select "+ ( ( ! q.exclusive ) ? "multiple " : "" )
                           + " form=\"mturk_form\" "
                           + " id=\"select_" + q.quid
                           + "\" name=\"" + q.quid
                           + "\" onchange='showNextButton(\"" + pid + "\", \"" + q.quid + "\", getDropdownOpt(\"" + q + "\"))'>"
                           + "<option disable selected>CHOOSE ONE</option>";
            for ( i = 0 ; i < q.options.length ; i++ ) {
                o = q.options[i];
                appendString = appendString
                               + "<option value='" + o.oid + ";" + questionsChosen.length + ";" + i
                               + "' id='" + o.oid
                               + "'>" + o.otext
                               + "</option>";
            }
            appendString = appendString + "</select>";
        } else {
            for ( i = 0 ; i < q.options.length ; i++) {
                o = q.options[i];
                appendString = appendString
                              + "<label for='" + oid + "'>"
                              + "<input type='" + ( q.exclusive ? 'radio' : 'check' )
                              + "' name='" + q.quid
                              + "' value='" + o.otext + ";" + questionsChosen.length + ";" + i
                              + "' id='" + o.oid
                              + "' onchange='showNextButton(\"" + pid + "\", \"" + q + "\", \"" + o + "\")' />"
                              + text + "</label>";
            }
        }
        return "<p id=\""+pid+"\">"+appendString+"</p>";
    };

    this.survey.randomize();

};

/*
{b1 : {id : "1.1.1"
        randomize : False
       questions: //top level questions as usual
       subblocks : {b1.1 : { ... }
       ...
       }


*/
