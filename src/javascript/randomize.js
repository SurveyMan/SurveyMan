// generate json (maps from before)
// instead of setting the vars directly, just provide them as args to these functions 
// and have the function set them

var SurveyMan = function (jsonSurvey) {

    var allQuestions        =   [],
        getQuestionById     =   function (quid) {

                                    var i;
                                    for ( i = 0 ; i < this.allQuestions.length ; i++ ) {
                                        if ( this.allQuestions[i].idString === quid ) {
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

                                    this.idString = jsonBlock.id;
                                    this.idArray = idStringToArray(this.idString);
                                    this.topLevelQuestions = Question.makeQuestions(jsonBlock.questions, this);
                                    this.branchParadigm = jsonBlock.branchParadigm;
                                    this.branchQuestion = this.getQuestion(jsonBlock.branchQuestionId);
                                    this.subblocks = [];
                                    this.randomizable = jsonBlock.randomize;
                                    this.getQuestion = function(quid) {
                                        var i;
                                        for ( i = 0 ; i < this.topLevelQuestions.length ; i++ ) {
                                            if ( this.topLevelQuestions[i].idString == quid ) {
                                                return this.topLevelQuestions[i];
                                            }
                                        }
                                        throw "Question with id " + quid + " not found in block " + this.idString;
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
                                        for ( i = 0 ; i < jsonBlock.subblocks.length ; i++ ) {
                                            var b = new Block(jsonBlock.subblocks[i]);
                                            this.subblocks.push(b);
                                            b.populate();
                                        }
                                    };
                                    // assert that the sub-blocks have the appropriate ids
                                    console.assert(_.every(subBlocks, function(b) { this.idComp(b) == 0 }));

                                },
        Option              =   function(jsonOption, _question) {

                                    var makeOptions = function (jsonOptions, enclosingQuestion) {
                                        var i, oList = [];
                                        for ( i = 0 ; i < jsonOptions.length ; i++ ){
                                            oList.push(new Option(jsonOptions[i], enclosingQuestion));
                                        }
                                        return oList;
                                    }

                                    this.idString = jsonOption.id;
                                    this.otext = jsonOption.otext;
                                    this.question = _question;

                                },
        Question            =   function(jsonQuestion, _block) {

                                    var makeQuestions   =   function (jsonQuestions, enclosingBlock) {
                                                                var i, qList = [];
                                                                for ( i = 0 ; i < jsonQuestions.length ; i++ ) {
                                                                    var q = new Question(jsonQuestions[i], enclosingBlock);
                                                                    qList.push(q);
                                                                    allQuestions.push(q);
                                                                }
                                                                return qList;
                                                            },
                                        makeBranchMap   =   function (branchMap, _question) {
                                                                var i, bm = {};
                                                                // branchMap -> map from oid to quid
                                                                if (!_.isUndefined(branchMap)) {
                                                                    var keys = _.branchMap.keys();
                                                                    for ( i = 0 ; i < keys.length ; i++ ) {
                                                                        var o = _question.getOption(keys[i]),
                                                                            q = getQuestionById(branchMap[keys[i]]);
                                                                        bm[o] = q;
                                                                    }
                                                                }
                                                                return bm;
                                                            };

                                    this.block = _block;
                                    this.idString = jsonQuestion.id;
                                    this.resource = jsonQuestion.resource;
                                    this.options = Option.makeOptions(jsonQuestion.options);
                                    this.branchMap = makeBranchMap(jsonQuestion.branchMap, this);
                                    this.randomizable = jsonQuestion.randomize;
                                    this.ordered = jsonQuestion.ordered;
                                    this.exclusive = jsonQuestion.exclusive;
                                    this.breakoff = jsonSurvey.breakoff;
                                    this.getOption = function (oid) {
                                        var i;
                                        for ( i = 0 ; i < options.length ; i++ ) {
                                            if ( options[i].idString === oid ) {
                                                return options[i];
                                            }
                                        }
                                        throw "Option id " + oid + " not found in question " + this.idString;
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

                                }
        Survey              =   function (jsonSurvey) {

                                    var makeSurvey = function(jsonSurvey.survey) {
                                        var i, blockList = [];
                                        for ( i = 0 ; i < jsonSurvey.length ; i++ ) {
                                            blockList[i] = new Block(jsonSurvey[i]).populate();
                                        }
                                    }

                                    this.filename = jsonSurvey.filename;
                                    this.topLevelBlocks = makeSurvey(jsonSurvey);
                                    this.randomize = function () {
                                        var i;
                                        for ( i = 0 ; i < this.topLevelBlocks.length ; i++ ) {
                                            // contents of the survey
                                            this.topLevelBlocks[i].randomize();
                                        }
                                    }

                                };

    this.survey = Survey(jsonSurvey);

};

/*
{b1 : {id : "1.1.1"
        randomize : False
       questions: //top level questions as usual
       subblocks : {b1.1 : { ... }
       ...
       }


*/

