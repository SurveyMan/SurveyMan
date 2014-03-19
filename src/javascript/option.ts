//TODO regex
//TODO other radio/check with text box
//TODO instead of answers knowing the correct option, options should know if they're correct
// then they can log correctness when they record themselves, simplifying training loop implementation
// and UI (bc otherwise have to get option id somehow)

/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

class ResponseOption{

    public text: string;
    public id: string; // must be to equal HTML
    public answer: string;

    constructor(jsonOption, public question: Question){
        jsonOption = _.defaults(jsonOption, {branchTo: null, answer: null});
        this.id = jsonOption.id;
        this.text = jsonOption.text;
        this.answer = jsonOption.answer;
    }

    public display(){}

    public getResponse(){
        return this.id;
    }

    public onChange(){
        if ($(OPTIONS+" :checked").length !== 0){
            this.question.enableNext();
        } else {
            this.question.disableNext();
        }
    }

    public selected(): boolean {
        var selectedIDs = _.pluck($(OPTIONS+" :checked"), "id");
        return _.contains(selectedIDs, this.id);
    }

    public getAnswer(){
        return this.answer; // answer must have text and id fields
    }

}

class RadioOption extends ResponseOption{
    display(){
        var label = document.createElement("label");
        $(label).attr("for", this.id);
        $(label).append(this.text);

        var input = document.createElement("input");
        $(input).attr({type: "radio", id: this.id, name: this.question.id});
        $(input).change((m:MouseEvent) => {this.onChange();});

        $(OPTIONS).append(label);
        $(OPTIONS).append(input);
    }

}

class CheckOption extends ResponseOption{
    display(){
        var label = document.createElement("label");
        $(label).attr("for", this.id);
        $(label).append(this.text);

        var input = document.createElement("input");
        $(input).attr({type: "checkbox", id: this.id, name: this.question.id});
        $(input).change((m:MouseEvent) => {this.onChange();});

        $(OPTIONS).append(label);
        $(OPTIONS).append(input);
    }
}

class TextOption extends ResponseOption{

    constructor(jsonOption, block){
        jsonOption = _.defaults(jsonOption, {text: ""});
        super(jsonOption, block);
    }

    display(){
        var input = document.createElement("input");
        $(input).attr({type: "text", id: this.id, name: this.question.id});
        $(input).keyup((m:MouseEvent) => {this.onChange();});

        $(OPTIONS).append(input);
    }

    public getResponse(){
        // return $("#"+this.id).val(); // TODO sometimes doesn't work and I don't know why
        return $(OPTIONS+" input").val();
    }

    public onChange(){
        if (this.getResponse()){
            this.question.enableNext();
        } else {
            this.question.disableNext();
        }
    }

    public selected(){
        return this.getResponse().length > 0;
    }

}

class DropDownOption extends ResponseOption{
    private exclusive: boolean;

    constructor(jsonOption, block, exclusive){
        super(jsonOption, block);
        this.exclusive = exclusive;
    }

    display(){
        //if select element exists, append to it, otherwise create it first
        if ($("select").length === 0){
            var select = document.createElement("select");
            if (!this.exclusive){
                $(select).attr({multiple: "multiple", name: this.question.id});
            }
            $(select).change((m:MouseEvent) => {this.onChange();});
            $(OPTIONS).append(select);
        }
        var option = document.createElement("option");
        $(option).attr("id", this.id);
        $(option).append(this.text);
        $("select").append(option);
    }

}
