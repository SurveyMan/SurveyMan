// generate json (maps from before)
// instead of setting the vars directly, just provide them as args to these functions
// and have the function set them

var SurveyMan = function (jsonSurvey) {

    // top-level variables used for state
    var questionMAP         =   {},
        optionMAP           =   {},
        blockMAP            =   {},
        questionSTACK       =   [],
        blockSTACK          =   [],
        questionsChosen     =   [],
        branchDest          =   null,
        dropdownThreshold   =   7,
        id                  =   0;

    // top-level aux functions

    var parseBools          =   function (thing, defaultVal) {
                                    if (_.isUndefined(thing)) {
                                        return defaultVal;
                                    } else if (typeof thing === "string") {
                                        return JSON.parse(thing);
                                    } else if (typeof thing === "boolean") {
                                        return thing
                                    } else throw "Unknown type for " + thing + " (" + typeof thing + ")";
                                },
        getOptionById       =   function (oid) {

                                    if (_.has(optionMAP, oid))
                                        return optionMAP[oid];
                                    else throw "Option id " + oid + " not found in optionMAP";

                                },
        getQuestionById     =   function (quid) {

                                    if (_.has(questionMAP, quid))
                                        return questionMAP[quid];
                                    else throw "Question id " + quid + " not found in questionMAP";

                                },
        getBlockById        =   function(bid){

                                    if (_.has(blockMAP, bid))
                                        return blockMAP[bid];
                                    else throw "Block id " + bid + " not found in blockMAP";

                                },
        getNextID           =   function() {

                                    id += 1;
                                    return "ans"+id;

                                },
        getDummyOpt         =   function(q) {

                                    return new Option({"id" : "comp_-1_-1", "otext" : ""}, q)

                                },
        logData             =   function (q, o) {

                                    console.log("getNextQuestion", questionSTACK.length);
                                    console.log("question ", q.qtext, q.id);
                                    if (o) console.log("option", o.otext, o.id);
                                },
        initializeStacks    =   function(_blist) {
                                    var topBlock;
                                    blockSTACK = _blist;
                                    topBlock = blockSTACK.shift();
                                    questionSTACK = topBlock.getAllBlockQuestions();
                                },
        isQuestionStackEmpty =   function () {
                                    return questionSTACK.length === 0;
                                },
        isBlockStackEmpty   =   function() {
                                    return blockSTACK.length === 0;
                                },
        loadQuestions       =   function(_qList) {
                                    questionSTACK = _qList;
                                },
        nextQuestion        =   function(){
                                    return questionSTACK.shift();
                                },
        nextBlock           =   function(){
                                    var head, b;
                                    if (branchDest) {
                                        while (!isBlockStackEmpty()) {
                                            head = blockSTACK.shift();
                                            if ( head === branchDest ) {
                                                blockSTACK.unshift(head);
                                                branchDest = null;
                                                break;
                                            } else if ( head.randomizable ) {
                                               blockSTACK.unshift(head);
                                               break;
                                            }
                                        }
                                    }
                                    console.assert( isQuestionStackEmpty() );
                                    b = blockSTACK.shift();
                                    loadQuestions(b.getAllBlockQuestions());
                                    return head;
                                },
        handleBranching     =   function (q, o){
                                    if (q.branchMap[o.id])
                                        branchDest = q.branchMap[o.id];
                                    if ( isQuestionStackEmpty() )
                                        nextBlock();
                                    return nextQuestion();
                                },
        nextSequential      =   function () {
                                    var b;
                                    if ( isQuestionStackEmpty() )
                                       nextBlock();
                                    return nextQuestion();
                                };


    //SurveyMan objects
    var Block               =   function(_jsonBlock) {

                                    blockMAP[_jsonBlock.id] = this;

                                    var idStringToArray = function (_idString) {
                                        return _.map(_idString.split("."), function(s) { parseInt(s); });
                                    };

                                    this.id = _jsonBlock.id;
                                    this.idArray = idStringToArray(_jsonBlock.id);
                                    this.topLevelQuestions = Question.makeQuestions(_jsonBlock.questions, this);
                                    this.subblocks = [];
                                    // may need to call a to boolean on jsonBlock.randomize
                                    this.randomizable = parseBools(_jsonBlock.randomize);
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
                                            indices = _.range(this.topLevelQuestions.length + this.subblocks.length),
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
                                        return _.flatten(retval);
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

                                        var i, j, newSBlocks = _.map(_.range(this.subblocks.length), function (foo) { return -1; });

                                        // randomize questions
                                        this.topLevelQuestions = _.shuffle(this.topLevelQuestions);

                                        // randomize options
                                        for (i = 0 ; i < this.topLevelQuestions.length ; i++ ) {
                                            this.topLevelQuestions[i].randomize();
                                        }

                                        if ( newSBlocks.length === 0 )
                                            return;

                                        // randomize blocks
                                        var stationaryBlocks = _.filter(this.subblocks, function (b) { return ! b.randomizable; }),
                                            nonStationaryBlocks = _.filter(this.subblocks, function (b) { return b.randomizable; }),
                                            samp = _.sample(_.range(this.subblocks.length), nonStationaryBlocks.length);

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

                                        if (_.isUndefined(_jsonBlock.subblocks)){
                                            console.log("No subblocks in Block " + this.id);
                                            return;
                                        }

                                        for ( i = 0 ; i < _jsonBlock.subblocks.length ; i++ ) {
                                            var b = new Block(_jsonBlock.subblocks[i]);
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
        Option              =   function(_jsonOption, _question) {

                                    optionMAP[_jsonOption.id] = this;

                                    this.id = _jsonOption.id;
                                    this.otext = _jsonOption.otext;
                                    this.question = _question;

                                },
        Question            =   function(_jsonQuestion, _block) {

                                    questionMAP[_jsonQuestion.id] = this;

                                    this.branchMap = {};

                                    this.makeBranchMap = function() {

                                        this.branchMap = function (_jsonBranchMap, _question) {

                                            var i, bm = {};
                                            // branchMap -> map from oid to bid
                                            if (!_.isUndefined(_jsonBranchMap)) {
                                                var keys = _.keys(_jsonBranchMap);
                                                for ( i = 0 ; i < keys.length ; i++ ) {
                                                    var o = _question.getOption(keys[i]),
                                                        b = getBlockById(_jsonBranchMap[keys[i]]);
                                                    bm[o.id] = b;
                                                }
                                                return bm;
                                            }

                                        }(_jsonQuestion.branchMap, this);

                                    };

                                    this.setFreetext = function (_jsonQuestion) {

                                        var reRe    =   new RegExp("#\{.*\}"),
                                            ft      =   _jsonQuestion.freetext;

                                        if ( ft == true ) {
                                            return true;
                                        } else if ( reRe.exec(ft) ) {
                                            return new RegExp(ft.substring(2, ft.length - 1));
                                        } else return new String(ft);

                                    };
                                    this.block = _block;
                                    this.id = _jsonQuestion.id;
                                    this.qtext = _jsonQuestion.qtext;
                                    this.freetext = parseBools(_jsonQuestion.freetext) ? this.setFreetext(_jsonQuestion) : Survey.freetextDefault;
                                    this.options = Option.makeOptions(_jsonQuestion.options, this);
                                    this.getOption = function (oid) {

                                        var i;
                                        for ( i = 0 ; i < this.options.length ; i++ ) {
                                            if ( this.options[i].id === oid ) {
                                                return this.options[i];
                                            }
                                        }
                                        throw "Option id " + oid + " not found in question " + this.id;

                                    };
                                    // FIELDS MUST BE SENT OVER AS STRINGS
                                    this.randomizable   =   parseBools(_jsonQuestion.randomize, Survey.randomizeDefault);
                                    this.ordered        =   parseBools(_jsonQuestion.ordered, Survey.orderedDefault);
                                    this.exclusive      =   parseBools(_jsonQuestion.exclusive, Survey.exclusiveDefault);
                                    this.breakoff       =   parseBools(_jsonQuestion.breakoff, Survey.breakoffDefault);
                                    this.randomize      =   function () {

                                        var i;

                                        if (!this.randomizable)
                                            return;

                                        if (this.ordered) {
                                            if (Math.random() < 0.5) {
                                                this.options = this.options.reverse();
                                            }
                                        } else {
                                            this.options = _.shuffle(this.options);
                                        }

                                    };


                                },
        Survey              =   function (_jsonSurvey) {

                                    var i, q;

                                    var makeSurvey = function(_jsonSurvey) {

                                        var i, blockList = [];
                                        for ( i = 0 ; i < _jsonSurvey.length ; i++ ) {
                                            blockList[i] = new Block(_jsonSurvey[i]);
                                            blockList[i].populate();
                                        }
                                        return blockList;

                                    };

                                    this.filename = _jsonSurvey.filename;
                                    this.topLevelBlocks = makeSurvey(_jsonSurvey.survey);
                                    for (i = 0 ; i < _.keys(questionMAP).length ; i++){
                                        q = _.values(questionMAP)[i];
                                        q.makeBranchMap();
                                    }
                                    this.breakoff = new Boolean(_jsonSurvey.breakoff);

                                };

    // "static" methods
    Survey.randomize        =   function (_survey) {

                                    var randomizableBlocks  =   _.shuffle(_.shuffle(_.filter(_survey.topLevelBlocks, function(_block) { return _block.randomizable; }))),
                                        normalBlocks        =   _.sortBy(_.filter(_survey.topLevelBlocks, function(_block) { return ! _block.randomizable }), function(_block) { return _block.id }),
                                        newTLBs             =   _.map(_.range(_survey.topLevelBlocks.length), function () { null }),
                                        indices             =   _.sortBy(_.sample(_.range(newTLBs.length), normalBlocks.length), function(n) { return n; }),
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
                                         questionMAP[q.id] = q;
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

    // "static" fields
    Survey.exclusiveDefault =   true;
    Survey.orderedDefault   =   false;
    Survey.randomizeDefault =   true;
    Survey.freetextDefault  =   false;
    Survey.breakoffDefault  =   true;
    Block.randomizeDefault  =   false;

    // display functions
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

                                        o.type = q.exclusive ? "radio" : "checkbox";
                                        o.id = opt.id;
                                        o.onchange = function () { sm.showNextButton(pid, q, opt) };
                                        o.name = q.id;
                                        o.value = JSON.stringify(retval);
                                        o.form = "mturk_form";
                                        console.log(o);

                                        return o;

                                    },
        SM                  =   {};
    SM.survey = new Survey(jsonSurvey);
    SM.showBreakoffNotice = function() {
        $(".question").append("<p>This survey will allow you to submit partial responses. The minimum payment is the quantity listed. However, you will be compensated more for completing more of the survey in the form of bonuses, at the completion of this study. The quantity paid depends on the results returned so far. Note that submitting partial results does not guarantee payment.</p>");
        $("div[name=question]").show();
        $(".question").append("<input type=\"button\" id=\"continue\" value=\"Continue\" onclick=\"sm.showFirstQuestion()\" />");
    };
    SM.finalSubmit = function () {
        return isQuestionStackEmpty() && isBlockStackEmpty();
    };
    SM.showFirstQuestion = function() {
        initializeStacks(SM.survey.topLevelBlocks);
        var firstQ = nextQuestion();
        SM.showQuestion(firstQ);
        SM.showOptions(firstQ);
    };
    SM.showSubmit = function(q,o) {
        var submitHTML;
        if (SM.submitNotYetShown()) {
            submitHTML = document.createElement("input");
            submitHTML.type = "submit";
            if ( SM.finalSubmit() ) {
                submitHTML.defaultValue = "Submit";
                submitHTML.id = "final_submit";
            } else if (SM.showEarlySubmit(q) && o) {
                submitHTML.defaultValue = "Submit Early";
                submitHTML.classList.add("breakoff");
                submitHTML.id = "submit_" + q.id;
            } else return;
            $("div[name=question]").append(submitHTML);
        }
    };
    SM.showQuestion =  function(q) {
        $(".question").empty();
        $(".question").append(q.qtext);
        $(".question").attr({ name : q.id });
    };
    SM.showOptions = function(q) {
        $(".answer").empty();
        var opts = SM.getOptionHTML(q);
        $(".answer").append(opts);
        if (SM.finalSubmit() && SM.submitNotYetShown())
            SM.showSubmit(q,true);
    };
    SM.showEarlySubmit = function (q) {
        return q.breakoff && q.options.length!=0;
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
        var submits = $(":submit");
        if (submits !== 0)
            console.log(submits);
        return submits.length === 0;
    };
    SM.showNextButton = function (pid, q, o) {
        var id, nextHTML;
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
        SM.showSubmit(q,o);
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
        $(par).attr({ name : q.id});

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

        } else if ( q.options.length == 0 ) {

            console.log("Instructional question");
            console.log("submit shown? " + ! SM.submitNotYetShown());
            console.log("final submit? " + SM.finalSubmit());
            console.log("size of qstack: " + questionSTACK.length + "size of block stack : " + blockSTACK.length);


            var dummy = getDummyOpt(q);

            if ( ! SM.finalSubmit() )
                SM.showNextButton(pid, q, dummy);
            SM.showSubmit(q, dummy);

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