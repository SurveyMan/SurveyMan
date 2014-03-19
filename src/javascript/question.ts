//TODO record time of presentation, time of clicking next, time of playing sound/video
//TODO compose questions from text and resources
//TODO correct answer can't be id for text box

/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

function record(text: string){ //TODO how to format form
    $(FORM).append(text);
}

class Page{
    public static dropdownThreshold: number = 7;
    public text: string;
    public id: string;
    public condition: string;
    public isLast: boolean;

    constructor(public block: Block){}

    public advance():void{}

    public disableNext(){
        $(CONTINUE).prop({disabled: true});
        if (this.isLast){
            $(BREAKOFF).show();
        }
    }

    public enableNext(){
        $(CONTINUE).prop({disabled: false});
        if (this.isLast){
            $(BREAKOFF).hide();
        }
    }

    public nextToSubmit(){
        $(CONTINUE).attr({type: "submit", value: "Submit", form: "surveyman"});
        $(CONTINUE).off('click');
    }

    public display(){
        if (this.isLast){
            this.nextToSubmit();
        } else {
            $(CONTINUE).off('click').click((m:MouseEvent) => {this.advance()});
        }
        this.disableNext();
        $(OPTIONS).empty();
        $(PAGE).empty().append(this.text);
    }

    public record(){
        $(FORM).append(this.id);
    }

}

class Question extends Page{
    private ordered: boolean;
    private exclusive: boolean;
    private freetext: boolean;
    private options: ResponseOption[];
    private answer: Statement;

    constructor(jsonQuestion, block){
        super(block);
        var jQuestion = _.defaults(jsonQuestion, {ordered: false, exclusive: true, freetext: false, answer: null, condition: null});
        this.id = jQuestion.id;
        this.text = jQuestion.text;
        this.condition = jQuestion.condition;
        this.ordered = jQuestion.ordered;
        this.exclusive = jQuestion.exclusive;
        this.freetext = jQuestion.freetext;
        if (jQuestion.answer){
            this.answer = new Statement({text: jQuestion.answer.text, id: jQuestion.answer.id}, // TODO answer id is id of correct option - is that problematic?
                    block);
        }
        this.options = _.map(jQuestion.options, (o):ResponseOption => {
            if (jQuestion.options.length > Page.dropdownThreshold){
                return new DropDownOption(o, this, this.exclusive);
            } else if (this.freetext){
                return new TextOption(o, this);
            } else if (this.exclusive){
                return new RadioOption(o, this);
            } else {
                return new CheckOption(o, this);
            }
        });
        this.orderOptions();
    }

    public display(): void{
        super.display();
        _.each(this.options, (o:ResponseOption):void => {o.display()});
    }

    public advance(): void{
        record(this.id);
        var selected = _.filter<ResponseOption>(this.options, (o) => {return o.selected()});
        _.each<ResponseOption>(selected, (s) => {record(s.getResponse())});

        var optAnswers = _.compact(_.map(selected, (o) => {return o.getAnswer();}));
        if (!_.isEmpty(optAnswers)){
            _.each(optAnswers, (ans) => {
                var optAnswer = new Statement(ans, this.block);
                optAnswer.display();
            });
        } else if (this.answer){
            this.answer.display();
        } else {
            this.block.advance();
        }
    }

    private orderOptions(): void{
        if (this.ordered){
            if (Math.random() > 0.5){
                this.options = this.options.reverse();
            }
        } else {
            this.options = _.shuffle<ResponseOption>(this.options);
        }
    }

}

class Statement extends Page{
    constructor(jsonStatement,
                block){
        super(block);
        var jStatement = _.defaults(jsonStatement, {condition: null});
        this.text = jsonStatement.text;
        this.id = jsonStatement.id;
        this.condition = jsonStatement.condition;
    }

    public display(){
        // super.display();
        if (this.isLast){
            this.nextToSubmit();
        } else {
            $(CONTINUE).off('click').click((m:MouseEvent) => {this.advance()});
        }
        this.disableNext();
        $(OPTIONS).empty();
        $(PAGE).empty().append(this.text);

        setTimeout(3000);//wait 3 seconds before enabling next
        this.enableNext();
    }

    public advance(){
        record(this.id);
        this.block.advance();
    }

}
