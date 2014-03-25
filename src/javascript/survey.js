// next features
//TODO interface with HTML, JSON, Java
//TODO create page html from text and resources; preload audio
//TODO placeholders
//TODO other radio/check with text box
// bug fixes
//TODO decide on interface for answers, fix display of multiple option's answers
//TODO allow runIf to be dependent on text response
//TODO make statement have delay before enabling Next
//TODO check different ways of accessing HTML
//TODO rename regex correct
//TODO make sure blockSize calculation is right
/// <reference path="container.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />
// global constants for referring to HTML
var PAGE = "p.question", OPTIONS = "p.answer", NAVIGATION = "div.navigation", CONTINUE = "#continue", BREAKOFF = "div.breakoff", FORM = "#surveyman";

var Survey = (function () {
    function Survey(jsonSurvey) {
        jsonSurvey = _.defaults(jsonSurvey, { breakoff: true, exchangeable: [] });
        this.exchangeable = jsonSurvey.exchangeable;
        this.showBreakoff = jsonSurvey.breakoff;
        this.contents = makeBlocks(jsonSurvey.blocks, this);
        this.contents = orderBlocks(this.contents, this.exchangeable);
    }
    Survey.prototype.start = function () {
        this.tellLast();
        this.makeNext();
        if (this.showBreakoff) {
            this.showBreakoffNotice();
        } else {
            this.advance();
        }
    };

    Survey.prototype.tellLast = function () {
        _.last(this.contents).tellLast();
    };

    Survey.prototype.makeNext = function () {
        var nextButton = document.createElement("input");
        $(nextButton).attr({ type: "button", id: "next", value: "Next" });
        $(NAVIGATION).append(nextButton);
    };

    Survey.prototype.showBreakoffNotice = function () {
        var breakoff = new Statement({ text: Survey.breakoffNotice, id: "breakoffnotice" }, this);
        var breakoffButton = document.createElement("input");
        $(breakoffButton).attr({ type: "submit", value: "Submit" });
        $(BREAKOFF).append(breakoffButton);
        breakoff.display();
    };

    Survey.prototype.advance = function () {
        if (!_.isEmpty(this.contents)) {
            var block = this.contents.shift();
            block.advance();
        }
    };
    Survey.breakoffNotice = "<p>This survey will allow you to " + "submit partial responses. The minimum payment is the quantity listed. " + "However, you will be compensated more for completing more of the survey " + "in the form of bonuses, at the completion of this study. The quantity " + "paid depends on the results returned so far. Note that submitting partial " + "results does not guarantee payment.</p>";
    return Survey;
})();
