//TODO other radio/check with text box

/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

class ResponseOption{

    public text: string;
    public id: string;
    public answer: string;
    public correct: boolean;

    constructor(jsonOption, public question: Question){
        jsonOption = _.defaults(jsonOption, {answer: null, correct: null});
        this.id = jsonOption.id;
        this.text = jsonOption.text;
        this.answer = jsonOption.answer;
        this.correct = jsonOption.correct; // has to be specified as false in the input for radio/check/dropdown if it should count as wrong
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
        if (this.answer){
            return new Statement({text: this.answer, id: 'ans_'+this.id}, this.question.block);
        } else {
            return null;
        }
    }

    public isCorrect(){
        return this.correct;
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
    private regex: RegExp;

    constructor(jsonOption, block){
        jsonOption = _.defaults(jsonOption, {text: "", regex: null});
        super(jsonOption, block);
        if (jsonOption.regex){
            this.regex = new RegExp(jsonOption.regex);
        }
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

    public isCorrect(){
        if (this.regex){
            return Boolean(this.getResponse().match(this.regex));
        } else {
            return null;
        }
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
