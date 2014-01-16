// generate json (maps from before)
// instead of setting the vars directly, just provide them as args to these functions 
// and have the function set them

var SurveyMan = function (jsonSurvey) {


    var Block = function(jsonBlock) {

        var idStringToArray = function (_idString) {
            return _.map(_idString.split("."), function(s) { parseInt(s); });
        }

        this.idString = jsonBlock.id;
        this.idArray = idStringToArray(this.idString);
        this.topLevelQuestions = Question.makeQuestions(jsonBlock.questions);
        this.subBlocks = jsonBlock.subblocks;
        this.randomizable = jsonBlock.randomize;
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
        }
        this.randomize = function () {
            // randomize questions
            _.shuffle(this.topLevelQuestions);
            // randomize blocks
            _.shuffle(subBlocks);
        }
        // assert that the sub-blocks have the appropriate ids
        console.assert(_.every(subBlocks, function(b) { this.idComp(b) == 0 }));
    }

    var Option = function(jsonOption, _question) {
        this.idString = jsonOption.id;
        this.otext = jsonOption.otext;
        this.question = _question;
    }

    var Question = function(jsonQuestion, _block) {
        this.block = _block;
        this.idString = jsonQuestion.id;
        this.resource = jsonQuestion.resource;
        this.options = Option.makeOptions(jsonQuestion.options);
        this.randomizable = jsonQuestion.randomize;
        this.ordered = jsonQuestion.ordered;
        this.exclusive = jsonQuestion.exclusive;
        this.randomize = function () {
            var i;
            if (this.ordered) {
                if (Math.random() < 0.5) {
                    options.reverse();
                }
            } else {
                _.shuffle(options);
            }
        }
    }

    var makeSurvey = function(jsonSurvey) {
        for ()
    }

    this.sourceFile = jsonSurvey.sourceFile;
    this.survey = makeSurvey(jsonSurvey.survey);

var makeBlockList = function(JSONSurvey) {
    randomizableBlocks = _.filter(JSONSurvey.keys(), function(m) { m['']})
    return _.shuffle()
}

var setVars = function (JSONSurvey) {
    // shuffle questions
    blockList = makeBlockList(JSONSurvey)
    questionList = []

    // pick blocks
    // every user has a unique view; keep views in a table, indexed by workerId hash
    // user-unique data : firstQuestionId, qTransTable, branchTable, oneBranchTable, lastQuestionId, bList
    // global data : qTable, oTable
    // remove the data when the worker submits.
     firstQuestionId =
     qTransTable = makeTransTable()
};

/*
{b1 : {id : "1.1.1"
        randomize : False
       questions: //top level questions as usual
       subblocks : {b1.1 : { ... }
       ...
       }


*/

