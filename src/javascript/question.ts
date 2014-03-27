/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

function record(pageRecord){
    var data = JSON.parse($(FORM).val());
    data.responses.push(pageRecord);
    $(FORM).val(JSON.stringify(data));
}

class Page{
    public static dropdownThreshold: number = 7;
    public text: string;
    public id: string;
    public condition: string;
    public isLast: boolean;
    public record;

    constructor(jsonPage, public block){
        jsonPage = _.defaults(jsonPage, {condition: null, resources: null});
        this.id = jsonPage.id;
        this.text = jsonPage.text;
        this.condition = jsonPage.condition;
        this.record = {page: this.id};
    }

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
        // $(CONTINUE).submit(); //TODO
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

}

class Question extends Page{
    private ordered: boolean;
    private exclusive: boolean;
    private freetext: boolean;
    private options: ResponseOption[];
    private answer: Statement;

    constructor(jsonQuestion, block){
        super(jsonQuestion, block);
        var jQuestion = _.defaults(jsonQuestion, {ordered: false, exclusive: true, freetext: false});
        this.ordered = jQuestion.ordered;
        this.exclusive = jQuestion.exclusive;
        this.freetext = jQuestion.freetext;
        if (jQuestion.answer){
            this.answer = new Statement({text: jQuestion.answer, id: 'ans_'+this.id}, block);
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
    }

    public display(): void{
        super.display();
        this.orderOptions();
        _.each(this.options, (o:ResponseOption):void => {o.display()});
        this.record['startTime'] = new Date().getTime();
    }

    public advance(): void{
        this.record['endTime'] = new Date().getTime();

        var selected: ResponseOption[] = _.filter<ResponseOption>(this.options, (o) => {return o.selected()});
        // answers that should be displayed to the respondent
        var optAnswers = _.compact(_.map(selected, (o) => {return o.getAnswer();}));

        this.recordResponses(selected);
        this.recordCorrect(selected);
        record(this.record);

        if (!_.isEmpty(optAnswers) && this.exclusive){ // ignoring option-by-option answers if nonexclusive
            optAnswers[0].display();
        } else if (this.answer){
            this.answer.display();
        } else {
            this.block.advance();
        }
    }

    public recordResponses(selected: ResponseOption[]){
        // ids of selections and value of text
        var responses: string[] = _.map(selected, (s) => {return s.getResponse()});
        this.record['selected'] = responses;
    }

    public recordCorrect(selected: ResponseOption[]){
        this.record['correct'] = _.map(selected, (s) => {return s.isCorrect()});
    }

    public orderOptions(): void{
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

    public display(){
        super.display();
        this.record['startTime'] = new Date().getTime();
        // setTimeout(() => {this.enableNext()}, 1000);
        this.enableNext();
    }

    public advance(){
        this.record['endTime'] = new Date().getTime();
        record(this.record);
        this.block.advance();
    }

}
