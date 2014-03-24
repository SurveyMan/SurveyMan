//TODO ability to distribute questions into blocks at runtime
//TODO ability to distribute resources among questions at runtime
// I'm thinking: the idea of a Placeholder, for questions or resources, that contains some sort of code that
// tells you which things can be chosen to fill it.

/// <reference path="survey.ts"/>
/// <reference path="container.ts"/>
/// <reference path="question.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

function getResponses(blockSize?){
    var data = JSON.parse($(FORM).val()).responses;
    return blockSize ? _.rest(data, data.length - blockSize) : data;
}

class Block{

    id: string;
    container: Container;
    contents;
    runIf: string;
    oldContents;

    constructor(jsonBlock){
        jsonBlock = _.defaults(jsonBlock, {runIf: null});
        this.runIf = jsonBlock.runIf;
        this.id = jsonBlock.id;
        this.oldContents = [];
    }


    tellLast(): void{}

    run(nextUp){}

    shouldRun(): boolean {
        if (this.runIf){
            var answersGiven = _.flatten(_.pluck(getResponses(), 'selected'));
            return _.contains(answersGiven, this.runIf);
        } else {
            return true;
        }
    }

    advance() {
        if (this.shouldRun() && !_.isEmpty(this.contents)){
            var nextUp = this.contents.shift();
            this.oldContents.push(nextUp);
            this.run(nextUp);
        } else {
            this.container.advance();
        }
    }
}

// an OuterBlock can only contain Blocks (InnerBlocks or OuterBlocks)
class OuterBlock extends Block implements Container{
    exchangeable;
    contents: Block[];

    constructor(jsonBlock, public container: Container){
        super(jsonBlock);
        jsonBlock = _.defaults(jsonBlock, {exchangeable: []});
        this.exchangeable = jsonBlock.exchangeable;
        this.contents = makeBlocks(jsonBlock.blocks, this);
        this.contents = orderBlocks(this.contents, this.exchangeable);
    }

    tellLast(){
        _.last<Block>(this.contents).tellLast();
    }

    run(block: Block){
        block.advance();
    }

}

// an InnerBlock can only contain Pages
class InnerBlock extends Block{
    contents: Page[];
    private latinSquare: boolean;
    private pseudorandom: boolean;
    private training: boolean;
    private criterion: number;

    constructor(jsonBlock, public container: Container){
        super(jsonBlock);
        jsonBlock = _.defaults(jsonBlock, {latinSquare: false, pseudorandomize: false, training: false, criterion: null});
        this.latinSquare = jsonBlock.latinSquare;
        this.pseudorandom = jsonBlock.pseudorandomize;
        this.training = jsonBlock.training;
        this.criterion = jsonBlock.criterion;
        if (jsonBlock.groups){
            this.contents = this.choosePages(jsonBlock.groups);
        } else {
            this.contents = this.makePages(jsonBlock.pages);
        }
        this.orderPages();
    }

    run(page: Page){
        page.display();
    }

    tellLast(){
        _.last(this.contents).isLast = true;
    }

    private makePages(jsonPages): Page[] {
        var pages = _.map<any,Page>(jsonPages, (p)=>{
            if (p.options){
                return new Question(p, this);
            } else {
                return new Statement(p, this);
            }
        });
        return pages;
    }

    private choosePages(groups): Page[] {
        var pages = this.latinSquare ? this.chooseLatinSquare(groups) : this.chooseRandom(groups);
        return this.makePages(pages);
    }

    private orderPages(): void{
        if (this.pseudorandom){
            this.pseudorandomize();
        } else {
            this.contents = _.shuffle<Page>(this.contents);
        }
    }

    private chooseLatinSquare(groups): any[]{
        var numConditions = groups[0].length;
        var lengths = _.pluck(groups, "length");
        var pages = [];
        if (_.every(lengths, (l:number):boolean => {return l === lengths[0]})){
            var version = _.random(numConditions - 1);
            for (var i = 0; i < groups.length; i++){
                var condition = (i + version) % numConditions;
                pages.push(groups[i][condition]);
            }
        } else {
            pages = this.chooseRandom(groups); // error passing silently - should be caught in Python
        }
        return pages;
    }

    private chooseRandom(groups): any[]{
         var pages = _.map(groups, (g)=>{return _.sample(g)});
         return pages;
    }

    private swapInto(nextP: Page, pages: Page[]): Page[]{
        for (var i = 0; i < pages.length; i++){
            var conds = _.pluck(pages.slice(i-1, i+2), 'condition');
            if (conds.every((c:string):boolean => {return c != nextP.condition})){
                var toSwap = pages[i];
                pages[i] = nextP;
                pages.push(toSwap);
            }
        }
        return pages;
    }

    private pseudorandomize(): void{
        var pages: Page[] = [];
        var remaining: Page[] = _.shuffle<Page>(this.contents);
        pages.push(remaining.shift());
        for (var i = 1; i < this.contents.length; i++){
            var validP: number = _.indexOf(remaining,
                    (p:Page):boolean => {return p.condition != pages[i-1].condition});
            if (validP > -1){
                pages.push(remaining.splice(validP, 1)[0]);
            } else {
                var nextP: Page = remaining.shift();
                pages = this.swapInto(nextP, pages);
            }
        }
        this.contents = pages;
    }

    // have to meet or exceed criterion to move on
    shouldLoop(): boolean {
        // will include Answers, but their correct will be null
        var blockResponses = getResponses(this.oldContents.length);

        //flatten is how I'm dealing with nonexclusive questions, not sure the best way
        var grades = _.flatten(_.pluck(blockResponses, 'correct'));

        //correct answers separated by questions with no specified answers count as in a row
        grades = _.reject<boolean[]>(grades, (g) => {return _.isNull(g)});
        var metric: number;

        // this.criterion is necessary percent correct
        if (this.criterion < 1){
            metric = _.compact(grades).length / grades.length;

        // this.criterion is necessary number correct in a row from the end
        } else {
            var lastIncorrect = _.lastIndexOf(grades, false);
            var metric = (lastIncorrect === -1) ? grades.length : grades.length - lastIncorrect;
        }

        return metric < this.criterion;
    }

    advance(){
        if (this.training && _.isEmpty(this.contents) && this.shouldLoop()){
            this.contents = this.oldContents;
            this.orderPages();
        }
        super.advance();
    }


}
