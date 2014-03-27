//TODO decide how to handle errors
//TODO make decimal criterion check against number of expected trues rather than number of questions
//TODO latin square using condition
//TODO interface with HTML, JSON, Java
//TODO create page html from text and resources; preload audio
//TODO placeholders
//TODO other radio/check with text box
//TODO allow runIf to be dependent on regex matching
//TODO matching any text ever given isn't very precise - change to match a certain option id and its text
//TODO maybe: allow option-by-option answers on nonexclusive questions
//TODO maybe: allow text box to start with text already in it
//TODO (css) spread checkboxes out by default, it's hard to know which label is for which box

/// <reference path="container.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

// global constants for referring to HTML
var PAGE = "p.question",
    OPTIONS = "p.answer",
    NAVIGATION = "div.navigation",
    CONTINUE = "#continue", // Next or Submit button
    BREAKOFF = "div.breakoff",
    FORM = "#surveyman";


class Survey implements Container{
    public exchangeable: string[];
    public contents: Block[];
    private showBreakoff: boolean;
    private static breakoffNotice: string = "<p>This survey will allow you to " +
        "submit partial responses. The minimum payment is the quantity listed. " +
        "However, you will be compensated more for completing more of the survey " +
        "in the form of bonuses, at the completion of this study. The quantity " +
        "paid depends on the results returned so far. Note that submitting partial " +
        "results does not guarantee payment.</p>";

    constructor(jsonSurvey){
        jsonSurvey = _.defaults(jsonSurvey, {breakoff: true, exchangeable: []});
        this.exchangeable = jsonSurvey.exchangeable;
        this.showBreakoff = jsonSurvey.breakoff;
        this.contents = makeBlocks(jsonSurvey.blocks, this);
        this.contents = orderBlocks(this.contents, this.exchangeable);
    }

    public start(){
        this.tellLast();
        this.makeNext();
        $('#surveyman').val(JSON.stringify({responses: []}));
        if (this.showBreakoff){
            this.showBreakoffNotice();
        } else {
            this.advance();
        }
    }

    private tellLast(){
        _.last<Block>(this.contents).tellLast();
    }

    private makeNext(){
        var nextButton = document.createElement("input");
        $(nextButton).attr({type: "button", id: "continue", value: "Next"});
        $(NAVIGATION).append(nextButton);
    }

    private showBreakoffNotice(){
        var breakoff = new Statement({text: Survey.breakoffNotice, id: "breakoffnotice"}, this);
        var breakoffButton = document.createElement("input");
        $(breakoffButton).attr({type: "submit", value: "Submit Early"});
        $(BREAKOFF).append(breakoffButton);
        breakoff.display();
    }

    public advance(){
        if (!_.isEmpty(this.contents)){
            var block = this.contents.shift();
            block.advance();
        }
    }

}
