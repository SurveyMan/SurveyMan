//TODO form; record time info
//TODO test branching, training loop
//TODO interface with HTML, JSON, Java
//TODO create page html from text and resources
//TODO placeholders
//TODO decide on interface for answers, fix display of multiple option's answers
//TODO preload audio
/// <reference path="container.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />
// global constants for referring to HTML
var PAGE = "p.question", OPTIONS = "p.answer", NAVIGATION = "div.navigation", CONTINUE = "#continue", BREAKOFF = "div.breakoff", FORM = "#surveyman";

/* make these divs for now, but should change the HTML to
already have them */
function setupHTML() {
    var nav = document.createElement("div");
    $(nav).attr("class", "navigation");

    var breakoff = document.createElement("div");
    $(breakoff).attr("class", "breakoff");

    var formdiv = document.createElement('div');
    $(formdiv).attr('class', 'responses');
    var form = document.createElement("form");
    var hidden = document.createElement("input");
    $(hidden).attr({ type: 'hidden', id: 'surveyman', value: JSON.stringify({ responses: [] }) });
    $(formdiv).append(form);
    $(form).append(hidden);
}

var Survey = (function () {
    function Survey(jsonSurvey) {
        jsonSurvey = _.defaults(jsonSurvey, { breakoff: true, exchangeable: [] });
        this.exchangeable = jsonSurvey.exchangeable;
        this.showBreakoff = jsonSurvey.breakoff;
        this.contents = makeBlocks(jsonSurvey.blocks, this);
        this.contents = orderBlocks(this.contents, this.exchangeable);
    }
    Survey.prototype.start = function () {
        setupHTML();
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
