// generate json (maps from before)
// instead of setting the vars directly, just provide them as args to these functions
// and have the function set them

var SurveyMan = function (jsonSurvey) {

    var allQuestions        =   [],
        currentQuestions    =   [],
        topBlocks           =   [],
        questionsChosen     =   [],
        dropdownThreshold   =   7,
        id                  =   0,
        getOptionById       =   function (oid) {

                                    var i,j,q,o;
                                    for ( i = 0 ; i < allQuestions.length ; i++ ) {
                                        q = allQuestions[i];
                                        for ( j = 0 ; j < q.options.length ; j++ ){
                                            o = q.options[j];
                                            if ( o.id === oid ) {
                                                return o;
                                            }
                                        }
                                    }

                                    throw "Option id " + oid + " not found in any of the options in allQuestions' options";

                                },
        getQuestionById     =   function (quid) {

                                    var i;
                                    for ( i = 0 ; i < allQuestions.length ; i++ ) {
                                        if ( allQuestions[i].id === quid ) {
                                            return allQuestions[i];
                                        }
                                    }

                                    throw "Question id " + quid + " not found in allQuestions";

                                },
        getBlockById        =   function(bid){

                                    var i, result;
                                    var getBlockByIdRec = function (_block, _bid) {
                                        var i, result;
                                        if (_bid === _block.id){
                                            return _block;
                                        } else {
                                            for ( i = 0 ; i < _block.subblocks.length ; i++ ) {
                                                result = getBlockByIdRec(_block.subblocks[i]);
                                                if (!_.isUndefined(result))
                                                    return result;
                                            }
                                            return;
                                        }
                                    };

                                    if (!_.isUndefined(bid)) {
                                        for ( i = 0 ; i < topBlocks.length ; i++ ) {
                                            result = getBlockByIdRec(topBlocks[i], bid);
                                            if (!_.isUndefined(result))
                                                return result;
                                        }
                                    }

                                    return;

                                },
        range               =   function (n) {

                                    var i, rList = [];
                                    for ( i = 0 ; i < n ; i++ ) {
                                        rList.push(i);
                                    }
                                    return rList;

                                },
        getNextID           =   function() {

                                    id += 1;
                                    return "ans"+id;

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
                                    this.randomizable = jsonBlock.randomize ? new Boolean(jsonBlock.randomize) : Block.randomizeDefault;
                                    this.isBranchAll = function () {
                                        var i, j, q, dests;
                                        if ( this.topLevelQuestions.length === 0 )
                                            return false;
                                        for ( i = 0 ; i < this.topLevelQuestions.length ; i++ ) {
                                            q = this.topLevelQuestions[i];
                                            if (q.branchMap) {
                                                dests = _.values(q.branchMap);
                                                if (! ( _.compact(dests).length === 0 ) ) {
                                                    return false;
                                                }
                                            } else return false;
                                        }
                                        return true;
                                    }
                                    this.getAllBlockQuestions = function () {
                                        // either one question is a branch or all, and they're always out of the top level block.
                                        // put the current block's questions in a global stack that we can empty
                                        //  how to interleave top-level questions and blocks?
                                        //  get the total number of "slots" and assign indices
                                        if (this.isBranchAll()) {
                                            return _.shuffle(this.topLevelQuestions)[0];
                                        }
                                        var i, j = 0, k = 0,
                                            retval = [],
                                            indices = range(this.topLevelQuestions.length + this.subblocks.length),
                                            qindices = _.sample(indices, this.topLevelQuestions.length),
                                            bindices = _.difference(indices, qindices);
                                        for ( i = 0 ; i < indices.length ; i++ ) {
                                          // it happens that i == indices[i]
                                          if (_.contains(qindices, i)) {
                                            retval.push(this.topLevelQuestions[j]);
                                            j++;
                                          } else if (_.contains(bindices, i)) {
                                            retval.push(this.subblocks[k].getAllBlockQuestions());
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
                                        for ( i = 0 ; i < this.idArray.length ; i++ ) {
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

                                        var i, j, newSBlocks = _.map(range(this.subblocks.length), function (foo) { return -1; });

                                        // randomize questions
                                        this.topLevelQuestions = _.shuffle(this.topLevelQuestions);

                                        // randomize options
                                        for (i = 0 ; i < this.topLevelQuestions.length ; i++ ) {
                                            this.topLevelQuestions[i].randomize();
                                        }

                                        if ( newSBlocks.length === 0 )
                                            return;

                                        // randomize blocks
                                        var stationaryBlocks = _.filter(this.subblocks, function (b) { return ! b.randomizable.valueOf(); }),
                                            nonStationaryBlocks = _.filter(this.subblocks, function (b) { return b.randomizable.valueOf(); }),
                                            samp = _.sample(range(this.subblocks.length), nonStationaryBlocks.length);

                                        nonStationaryBlocks = _.shuffle(nonStationaryBlocks);

                                        for ( i = 0 ; i < samp.length ; i++ ) {
                                            // pick the locations for where to put the non-stationary blocks
                                            newSBlocks[samp[i]] = nonStationaryBlocks[i];
                                        }
                                        for ( i = 0, j = 0; i < newSBlocks.length ; i++ ) {
                                            if ( newSBlocks[i] == -1 ) {
                                                newSBlocks[i] = stationaryBlocks[j];
                                                j++;
                                            }
                                        }

                                        console.assert(j == stationaryBlocks.length);

                                        this.subblocks = newSBlocks;

                                        for ( i = 0 ; i < this.subblocks.length ; i++) {
                                            this.subblocks[i].randomize();
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
                                    this.getFirstQuestion = function () {
                                        var i;
                                        if (this.topLevelQuestions.length!=0)
                                            return this.topLevelQuestions[0];
                                        if (this.subblocks.length === 0 )
                                            throw "Malformed survey; empty block stack ending in " + this.id;
                                        return this.subblocks[0].getFirstQuestion();
                                    }
                                    // assert that the sub-blocks have the appropriate ids
                                    console.assert(_.every(this.subBlocks, function(b) { return this.idComp(b) === 0 }));

                                },
        Option              =   function(jsonOption, _question) {

                                    this.id = jsonOption.id;
                                    this.otext = jsonOption.otext;
                                    this.question = _question;

                                },
        Question            =   function(jsonQuestion, _block) {


                                    var makeBranchMap   =   function (jsonBranchMap, _question) {
                                                                var i, bm = {};
                                                                // branchMap -> map from oid to bid
                                                                if (!_.isUndefined(jsonBranchMap)) {
                                                                    var keys = _.keys(jsonBranchMap);
                                                                    for ( i = 0 ; i < keys.length ; i++ ) {
                                                                        var o = _question.getOption(keys[i]),
                                                                            b = getBlockById(jsonBranchMap[keys[i]]);
                                                                        bm[o.id] = b;
                                                                    }
                                                                    return bm;
                                                                }
                                                            };

                                    this.makeBranchMap = function() { this.branchMap = makeBranchMap(jsonQuestion.branchMap, this) };
                                    this.setFreetext = function (_jsonQuestion) {

                                        var reRe    =   new RegExp("#\{.*}"),
                                            ft      =   _jsonQuestion.freetext;

                                        if ( ft == true ) {
                                            return true;
                                        } else if ( reRe.exec(ft) ) {
                                            return new RegExp(ft.substring(2, ft.length - 1));
                                        } else return new String(ft);

                                    };
                                    this.block = _block;
                                    this.id = jsonQuestion.id;
                                    this.qtext = jsonQuestion.qtext;
                                    this.freetext = jsonQuestion.freetext ? this.setFreetext(jsonQuestion) : Survey.freetextDefault;
                                    this.options = Option.makeOptions(jsonQuestion.options, this);
                                    this.getOption = function (oid) {

                                        var i;
                                        for ( i = 0 ; i < this.options.length ; i++ ) {
                                            if ( this.options[i].id === oid ) {
                                                return this.options[i];
                                            }
                                        }
                                        throw "Option id " + oid + " not found in question " + this.id;

                                    };

                                    this.branchMap = makeBranchMap(jsonQuestion.branchMap, this);
                                    // FIELDS MUST BE SENT OVER AS STRINGS
                                    this.randomizable = jsonQuestion.randomize || Survey.randomizeDefault;
                                    this.ordered = jsonQuestion.ordered || Survey.orderedDefault;
                                    this.exclusive = jsonQuestion.exclusive || Survey.exclusiveDefault;
                                    this.breakoff = jsonQuestion.breakoff || Survey.breakoffDefault;
                                    this.randomize = function () {

                                        var i;
                                        if (this.ordered) {
                                            if (Math.random() < 0.5) {
                                                this.options = this.options.reverse();
                                            }
                                        } else {
                                            this.options = _.shuffle(this.options);
                                        }

                                    };

                                },
        Survey              =   function (jsonSurvey) {

                                    var i;

                                    var makeSurvey = function(jsonSurvey) {

                                        var i, blockList = [];
                                        for ( i = 0 ; i < jsonSurvey.length ; i++ ) {
                                            blockList[i] = new Block(jsonSurvey[i]);
                                            blockList[i].populate();
                                        }
                                        return blockList;

                                    };

                                    this.filename = jsonSurvey.filename;
                                    this.topLevelBlocks = makeSurvey(jsonSurvey.survey);
                                    topBlocks = this.topLevelBlocks;
                                    for ( i = 0 ; i < allQuestions.length ; i++ ) {
                                        allQuestions[i].makeBranchMap();
                                    }
                                    this.breakoff = new Boolean(jsonSurvey.breakoff);
                                    this.getFirstQuestion = function () {
                                        return this.topLevelBlocks[0].getFirstQuestion();
                                    };

                                };
    Survey.randomize        =   function (_survey) {

                                    var randomizableBlocks  =   _.shuffle(_.shuffle(_.filter(_survey.topLevelBlocks, function(_block) { return _block.randomizable; }))),
                                        normalBlocks        =   _.filter(_survey.topLevelBlocks, function(_block) { return ! _block.randomizable }),
                                        newTLBs             =   _.map(range(_survey.topLevelBlocks.length), function () { null }),
                                        indices             =   _.sortBy(_.sample(range(newTLBs.length), normalBlocks.length), function(n) { return n; }),
                                        i, j, k = 0;

                                    // randomize new TLBs as appropriate

                                    for ( j = 0 ; j < indices.length ; j++ ) {
                                        newTLBs[indices[j]] = normalBlocks[j];
                                    }

                                    for ( i = 0 ; i < newTLBs.length ; i++ ) {
                                        if (_.isUndefined(newTLBs[i])) {
                                            newTLBs[i] = randomizableBlocks[k];
                                            k++;
                                        }
                                    }

                                    // reset top level blocks
                                    _survey.topLevelBlocks = newTLBs;

                                    for ( i = 0 ; i < _survey.topLevelBlocks.length ; i++ ) {
                                        // contents of the survey
                                        _survey.topLevelBlocks[i].randomize();
                                    }

                                    //_survey.firstQuestion = _survey.getFirstQuestion();
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
                                     var i, oList = [];
                                     if (_.isUndefined(jsonOptions)) {
                                        var obj = _.keys(enclosingQuestion), str = "";
                                        for (i = 0 ; i < obj.length ; i++) {
                                            str += "\t" + obj[i] + ":" + enclosingQuestion[obj[i]] ;
                                        }
                                        console.log("No options defined for " + enclosingQuestion.id + " (" + str + ")");
                                        console.assert(enclosingQuestion.freetext);
                                        return;
                                     }
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

    var makeDropDown        =   function (q, opt, qpos, opos) {

                                        var retval  =   {"quid" : q.id, "oid" : opt.id, "qpos" : qpos, "opos" : opos},
                                            o       =   document.createElement('option');

                                        o.text = opt.otext;
                                        o.value = JSON.stringify(retval);
                                        o.id = opt.id;

                                        return o;

                                    },
        makeRadioOrCheck    =   function (pid, q, opt, qpos, opos) {

                                        var retval  =   {"quid" : q.id, "oid" : opt.id, "qpos" : qpos, "opos" : opos},
                                            o       =   document.createElement("input");

                                        o.type = q.exclusive ? "radio" : "check";
                                        o.id = opt.id;
                                        o.onchange = function () { sm.showNextButton(pid, q, opt) };
                                        o.name = q.id;
                                        o.value = JSON.stringify(retval);
                                        o.form = "mturk_form";
                                        console.log(o);

                                        return o;

                                    },
        logData             =   function (q, o) {
                                    console.log("getNextQuestion", currentQuestions.length);
                                    console.log("question ", q.qtext, q.id);
                                    if (o) console.log("option", o.otext, o.id);
                                },
        isCurrentBlockEmpty =   function () {
                                    return currentQuestions.length === 0;
                                },
        handleBranching     =   function (q, o){
                                    var b, head;
                                    if (q.branchMap[o.id]) {
                                        // get the block we're branching to
                                        b = q.branchMap[o.id];
                                        // pop off all top level blocks until we reach this block
                                        while (topBlocks.length != 0) {
                                            head = topBlocks.shift();
                                            if ( head === b ) {
                                                topBlocks.unshift(head);
                                                break;
                                            } else if ( head.randomize ) {
                                                topBlocks = topBlocks + [ head ];
                                            }
                                        }
                                        if ( isCurrentBlockEmpty() )
                                            currentQuestions = b.getAllBlockQuestions();
                                    } else if ( isCurrentBlockEmpty() ) {
                                        // randomizable blocks; these are pre-selected at the enclosing block level
                                        if (topBlocks.length > 0) {
                                            b = topBlocks.shift();
                                            currentQuestions = b.getAllBlockQuestions();
                                        }
                                        else return;
                                    }
                                    return currentQuestions.shift();
                                },
        nextSequential      =   function () {
                                    var b;
                                    if ( isCurrentBlockEmpty() ) {
                                        // should never be called on empty topBlocks
                                        // if we're out of questions, get the questions from the next block
                                        b = topBlocks.shift();
                                        currentQuestions = b.getAllBlockQuestions();
                                    }
                                    return currentQuestions.shift();
                                },
        SM                  =   {};
    SM.survey = new Survey(jsonSurvey);
    SM.showBreakoffNotice = function() {
        $(".question").append("<p>This survey will allow you to submit partial responses. The minimum payment is the quantity listed. However, you will be compensated more for completing more of the survey in the form of bonuses, at the completion of this study. The quantity paid depends on the results returned so far. Note that submitting partial results does not guarantee payment.</p>");
        $("div[name=question]").show();
        $(".question").append("<input type=\"button\" value=\"Continue\" onclick=\"sm.showFirstQuestion()\" />");
    };
    SM.showFirstQuestion = function() {
        topBlocks = SM.survey.topLevelBlocks;
        currentQuestions = topBlocks[0].getAllBlockQuestions();
        var firstQ = currentQuestions.shift();
        SM.showQuestion(firstQ);
        SM.showOptions(firstQ);
        topBlocks.shift();
    };
    SM.showQuestion =  function(q) {
        $(".question").empty();
        $(".question").append(q.qtext);
    };
    SM.showOptions = function(q) {
        $(".answer").empty();
        $(".answer").append(SM.getOptionHTML(q));
    };
    SM.showEarlySubmit = function (q) {
        return q.breakoff;
    };
    SM.getNextQuestion = function (q, o) {
        logData(q,o);
        var nextQ;
        if (o && !_.isUndefined(q.branchMap)) {
            // returns a block
            console.log("branching in question " + q.id + "(" + q.qtext + ")");
            nextQ = handleBranching(q, o);
        } else {
            // get the next sequential question
            console.log("get next sequential question after " + q.id + "(" + q.qtext + ")" );
            nextQ = nextSequential();
        }
        return nextQ;
    };
    SM.registerAnswerAndShowNextQuestion = function (pid, q, o) {
        // if we're coming from an instructional question, just skip registering
        if (o) {
            $("form").append($("#"+pid));
            $("#"+pid).hide();
        }
        questionsChosen.push(q);
        console.log(pid, q.id, o?o.id:"");
        $("#next_"+q.id).remove();
        $("#submit_"+q.id).remove();
        q = SM.getNextQuestion(q, o);
        SM.showQuestion(q);
        SM.showOptions(q);
    };
    SM.submitNotYetShown = function () {
        return $(":submit").length === 0;
    };
    SM.finalSubmit = function () {
        return currentQuestions.length === 0 && topBlocks.length === 0
    };
    SM.showNextButton = function (pid, q, o) {
        var id, nextHTML, submitHTML;
        id = "next_"+q.id;
        console.log(id, $("#"+id).length);
        if ($("#" + id).length > 0)
            document.getElementById(id).onclick = function () { sm.registerAnswerAndShowNextQuestion(pid, q, o); };
        else if ( ! SM.finalSubmit() ) {
            nextHTML = document.createElement("input");
            nextHTML.id = id;
            nextHTML.type = "button";
            nextHTML.onclick = function () { sm.registerAnswerAndShowNextQuestion(pid, q, o); };
            nextHTML.value = "Next";
            $("div[name=question]").append(nextHTML);
        }
        if (SM.submitNotYetShown() && o) {
            submitHTML = document.createElement("input");
            submitHTML.type = "submit";
            if ( SM.finalSubmit() ) {
                submitHTML.defaultValue = "Submit";
                submitHTML.id = "final_submit";
            } else if (SM.showEarlySubmit(q)) {
                submitHTML.defaultValue = "Submit Early";
                submitHTML.classList.add("breakoff");
                submitHTML.id = "submit_" + q.id;
            } else return;
            $("div[name=question]").append(submitHTML);
        }
    };
    SM.getDropdownOpt = function(q) {
        var dropdownOpt =   $("#select_" + q.id + " option:selected");
        var    oid         =   dropdownOpt.attr("id");
        var    opt         =   getOptionById(oid);
        return opt;
    };
    SM.getOptionHTML = function (q) {
        // would like to replace text area, select, etc. with JS objects
        var opt, i, elt, dummy, retval,
            qpos    =   questionsChosen.length,
            pid     =   getNextID(),
            par     =   document.createElement("p");

        par.id = pid;
        if ( q.freetext ) {

            elt = document.createElement("textarea");
            elt.id = q.id;
            elt.type = "text";
            elt.oninput = function () {
                if ( q.freetext instanceof RegExp ) {
                    var inputText = document.getElementById(elt.id).value;
                    if ( q.freetext.test(inputText) )
                        sm.showNextButton(pid, q, -1);
                } else sm.showNextButton(pid, q, -1);
            };
            elt.name  = q.id;
            if (q.freetext instanceof String)
                elt.defaultValue = q.freetext;
            elt.form = "mturk_form";
            $(par).append(elt);

        } else if ( q.options.length > dropdownThreshold ) {

            elt = document.createElement("select");
            elt.id = "select_" + q.id;
            elt.onchange = function () { sm.showNextButton(pid, q, sm.getDropdownOpt(q)); };
            $(elt).attr({ name : q.id, form : "mturk_form" });
            if (!q.exclusive) {
                $(elt).prop("multiple", true);
            }

            dummy = document.createElement('option');
            dummy.text = "CHOOSE ONE";
            $(dummy).attr({disable : true, selected : true});
            elt.options[elt.options.length] = dummy;

            for ( i = 0 ; i < q.options.length ; i++ ) {
                opt = q.options[i];
                elt.add(makeDropDown(q, opt, qpos, i));
            }

            $(par).append(elt);

        } else {

            for ( i = 0 ; i < q.options.length ; i++) {

                if (q.options.length === 1 && q.options[0].otext === "null") {
                    // no options; just display next
                    SM.showNextButton(null, q, null);
                    return "";
                }

                opt = q.options[i];
                elt = document.createElement("label");
                $(elt).attr("for", opt.oid);
                $(elt).append(makeRadioOrCheck(pid, q, opt, qpos, i));
                $(elt).append(opt.otext);
                $(par).append(elt);
            }
        }
        return par;
    };
    SM.randomize = function () {
        Survey.randomize(SM.survey);
    };

    return SM;

};