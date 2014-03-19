
test("create inner block", function(){
    var jsonb = {id: "b1", pages:[{text:"one", id:"p1"}, {text:"two", id:"p2", freetext: true, options: [{id: "o1"}]}]};
    var b = new InnerBlock(jsonb);
    strictEqual(b.contents.length, 2, "are pages initialized properly?");
    _.each(b.contents, function(p){
        if (p.id === "p1"){
            ok(p instanceof Statement, "should be a Statement");
        } else {
            ok(p instanceof Question, "should be a Question");
        }
    });
    strictEqual(b.runIf, null, "runIf default not working properly");
    strictEqual(b.shouldRun(), true, "shouldRun not defaulting to true");
    jsonb.runIf = "o3";
    var b2 = new InnerBlock(jsonb);
    strictEqual(b2.runIf, "o3", "runIf not set properly");
    strictEqual(b2.shouldRun(), false, "shouldRun not working");
    var jsongroup = {id: "b1", groups:[[{text: "page1", id: "p1"}, {text:"page2", id:"p2", options: [{id: "o1"}]}]]};
    var b3 = new InnerBlock(jsongroup);
    strictEqual(b3.contents.length, 1, "initialization from groups");
    _.each(b3.contents, function(p){
        if (p.id === "p1"){
            ok(p instanceof Statement, "should be a Statement");
        } else {
            ok(p instanceof Question, "should be a Question");
        }
    });
});

test("choosing pages", function(){
    var grps = _.map(_.range(6), function(i){
                                    return _.map(_.range(3), function(j){
                                                          return {text: "page "+(i*10 + j).toString(), id: (i*10+j).toString(), condition: j.toString()};
                                                      }
                                          );
                                 }
                    );
    var jsonb = {id: "b1", groups: grps};
    var b = new InnerBlock(jsonb);
    strictEqual(b.contents.length, 6, "did it choose one page per group?");
    var choices = (_.map(_.range(10), function(i){
            var bl = new InnerBlock(jsonb);
            return _.pluck(bl.contents, "id");
        }));
    ok(_.each(_.range(18), function(id){return _.contains(_.flatten(choices), id.toString());}), "no page is systematically avoided in random sampling");
    jsonb.latinSquare = true;
    var b2 = new InnerBlock(jsonb);
    strictEqual(b2.latinSquare, true, "latin square variable getting set");
    strictEqual(b2.contents.length, 6, "did it choose one page per group with latinSquare set to true?");
    var condgroups = _.groupBy(b2.contents, "condition");
    ok(_.every(condgroups, function(g){return g.length === 2;}), "check latin square");
});

test("ordering pages", function(){
    var grps = _.map(_.range(6), function(i){
                                    return _.map(_.range(3), function(j){
                                                          return {text: "page "+(i*10 + j).toString(), id: (i*10+j).toString(), condition: j.toString()};
                                                      }
                                          );
                                 }
                    );
    var jsonb = {id: "b1", groups: grps};
    var firstconditions = _.map(_.range(20), function(){
        var b = new InnerBlock(jsonb);
        var conditions = _.pluck(b.contents, "condition");
        return conditions[0];
    });
    strictEqual(_.unique(firstconditions).length, 3, "any condition should be able to end up first (but randomness means this will fail occasionally)");

    jsonb.pseudorandomize = true;
    var b2 = new InnerBlock(jsonb, {});
    var conditions2 = _.pluck(b2.contents, "condition");
    var firstconditions2 = _.map(_.range(20), function(){
        var b = new InnerBlock(jsonb);
        var conditions = _.pluck(b.contents, "condition");
        return conditions[0];
    });

    strictEqual(_.unique(firstconditions2).length, 3, "any condition should be able to end up first (but this is random)");
    var adjacentconditions = _.zip(b2.contents, _.rest(b2.contents));
    var clashes = _.filter(adjacentconditions, function(pair){return pair[0] === pair[1];});
    strictEqual(clashes.length, 0, "no two adjacent conditions should be the same when pseudorandomized (deterministic)");
});


function clickNext(){$(":button").trigger("click");}
function CustomError( message ) {
    this.message = message;
}

CustomError.prototype.toString = function() {
    return this.message;
};

var fakeContainer = {advance: function(){throw new CustomError("I advanced");}};

test("statement calling advance", function(){
    setup();
    var pgs = [{text:"page1", id:"p1"}, {text:"page2", id:"p2"}];
    var b = new InnerBlock({id:"b1", pages: pgs} , fakeContainer);
    b.advance();

    // first statement displays
    var text1 = $("p.question").text();
    notEqual(text1, "", "statement should display");
    strictEqual($(":button").length, 1, "there should be a next button");

    clickNext();

    // second statement displays
    notEqual(text1, $("p.question").text(), "next statement should display after click");

    // out of pages so Next should call Page's advance which will call Block's advance which will call its
    // container's advance, which is here set to throw an error
    throws(clickNext, CustomError, "at end of block, block's container's advance should be called");
});

test("question calling advance", function(){
    setup();
    var pgs = [{text:"page1", id:"p1", freetext: true, options:[{id: "o1"}] },
        {text:"page2", id:"p2", freetext: true, options:[{id:"o2"}] }];
    var b = new InnerBlock({id:"b1", pages: pgs}, fakeContainer);
    b.advance();

    var text1 = $("p.question").text();
    notEqual(text1, "", "question text should display");
    strictEqual($(":button").prop("disabled"), true, "next button should be disabled");
    strictEqual($("#continue").prop("disabled"), true, "next button should be disabled");

    // currently displayed page is at end of block's oldContents now
    var oid = b.oldContents[0].options[0].id;

    strictEqual($("#"+oid).val(), '', "text box should be empty");
    strictEqual($("p.answer input").val(), '', "text box should be empty");
    strictEqual(b.oldContents[0].options[0].selected(), false, "option should know it's unselected");

    // enable Next button - XXX not needed here but was needed in question testing, don't know why
    // $(":input[type='text']").val("hi");
    // $(":input[type='text']").trigger("keyup");
    clickNext();

    notEqual(text1, $("p.question").text(), "next question text should display after click");
    notEqual("", $("p.question").text(), "next question text should display after click");
    strictEqual($("p.answer input").val().length, 0, "text box should be empty");
    strictEqual(b.oldContents[1].options[0].selected(), false, "option should know it's unselected");

    throws(clickNext, CustomError, "at end of block, block's container's advance should be called");
});

test("question with answer calling advance", function(){
    setup();
    var pgs = [{text:"page1", id:"p1", freetext: true, options:[{id: "o1"}] , answer:{text:"good job", id:"a1"} } ,
        {text:"page2", id:"p2", freetext: true, options:[{id:"o2"}] , answer:{text: "great job", id:"a1"} }];
    var b = new InnerBlock({id:"b1", pages: pgs}, fakeContainer);
    b.advance();

    var text1 = $("p.question").text();
    strictEqual($("p.question:contains('page')").length, 1, "question text should display");

    // enable Next button
    $(":input[type='text']").val("hi");
    // $(":input[type='text']").trigger("keyup");
    clickNext();

    strictEqual($("p.question:contains('job')").length, 1, "answer should display after click");

    clickNext();

    notEqual(text1, $("p.question").text(), "next question text should display after click");
    strictEqual($("p.question:contains('page')").length, 1, "next question text should display after click");

    $(":input[type='text']").val("hi");
    $(":input[type='text']").trigger("keyup");
    clickNext();

    strictEqual($("p.question:contains('job')").length, 1, "answer should display after click");

    throws(clickNext, CustomError, "at end of block, block's container's advance should be called");

});

test("question with options with answers calling advance", function(){

});

var pgs = [{text:"page1", id:"p1", options:[{id: "o1", text: "A"}, {id:'o2', text:"B"}] , answer:{text:"good job", id:"o1"} },
    {text:"page2", id:"p2", options:[{id: "o3", text: "A"}, {id:'o4', text:"B"}], answer:{text: "great job", id:"a1"} }];
var pgs2 = [{text:"page3", id:"p3", options:[{id: "o5", text: "A"}, {id:'o6', text:"B"}] , answer:{text:"good job", id:"a3"} },
    {text:"page4", id:"p4", options:[{id: "o7", text: "A"}, {id:'o8', text:"B"}], answer:{text: "great job", id:"a4"} }];
var pgs3 = [{text:"page5", id:"p5", options:[{id: "o9", text: "A"}, {id:'o10', text:"B"}] , answer:{text:"good job", id:"a5"} },
    {text:"page6", id:"p6", options:[{id: "o11", text: "A"}, {id:'o12', text:"B"}], answer:{text: "great job", id:"a6"} }];
var pgs4 = [{text:"page8", id:"p8", options:[{id: "o13", text: "A"}, {id:'o14', text:"B"}] , answer:{text:"good job", id:"a8"} },
    {text:"page9", id:"p9", options:[{id: "o15", text: "A"}, {id:'o16', text:"B"}], answer:{text: "great job", id:"a9"} }];

var jsons = {blocks:[
    { id:'b1', pages: pgs },
    { id:'b2', exchangeable: ['b3', 'b4'], blocks: [
        { id:'b3', pages: pgs2 },
        { id:'b4', pages: pgs3 }
    ]}
] };

test('create outerblock', function(){
    var jsonb = {id:'b1', blocks:[
            { id:'b3', pages: pgs2 },
            { id:'b4', pages: pgs3 }
            ]};
    var b = new OuterBlock(jsonb, fakeContainer);
    strictEqual(b.id, 'b1', 'block id should be set');
    strictEqual(b.exchangeable.length, 0, 'exchangeable default should be set');
    strictEqual(b.runIf, null, 'runIf default should be set');
    strictEqual(b.contents.length, 2, 'contents should be set');
    ok(b.contents[0] instanceof InnerBlock, 'contents should be InnerBlock');

    jsonb2 = $.extend(true, {}, jsonb);
    jsonb2.exchangeable = ['b3', 'b4'];
    var b2 = new OuterBlock(jsonb2, fakeContainer);
    deepEqual(b2.exchangeable, ['b3', 'b4'], 'exchangeable should be set when passed in');

    jsonb2.runIf = 'o1';
    var b3 = new OuterBlock(jsonb2, fakeContainer);
    strictEqual(b3.runIf, 'o1', 'runIf should be set when passed in');
});


test("create survey", function(){
    setup();
    var s = new Survey(jsons);

    strictEqual(s.contents.length, 2, "survey should have two blocks");
    ok(s.contents[0] instanceof InnerBlock, 'survey should detect that first block is inner block');
    ok(s.contents[1] instanceof OuterBlock, 'survey should detect that second block is outer block');
    strictEqual(s.exchangeable.length, 0, 'exchangeable should be set to default');
    strictEqual(s.showBreakoff, true, 'showBreakoff should be set to default');
});

test("order blocks", function(){
    var b1 = new InnerBlock({id: 'b1', pages: pgs}, fakeContainer);
    var b2 = new OuterBlock({id: 'b2', blocks: [ {id: 'b3', pages: pgs2}, {id: 'b4', pages: pgs3} ] }, fakeContainer);
    var b3 = new InnerBlock({id: 'b5', pages: pgs4 }, fakeContainer);

    function testOrderBlocks(exchangeable){
        var blockOrders = _.map(_.range(15), function(i){
            return orderBlocks([b1, b2, b3], exchangeable);
        });
        var blockIds = _.map(blockOrders, function(bo){
            return _.pluck(bo, 'id');
        });
        return blockIds;
    }

    var blockIds1 = testOrderBlocks([]);
    var matching1 = _.map(blockIds1, function(bi){
        return (bi[0] === 'b1') && (bi[1] === 'b2') && (bi[2] === 'b5');
    });
    strictEqual(_.compact(matching1).length, matching1.length, 'with no exchangeable blocks, block order should stay the same');

    var blockIds2 = testOrderBlocks(['b1']);
    var matching2 = _.map(blockIds2, function(bi){
        return (bi[0] === 'b1') && (bi[1] === 'b2') && (bi[2] === 'b5');
    });
    strictEqual(_.compact(matching2).length, matching2.length, 'with one exchangeable block (with nowhere to go), block order should stay the same');

    function moveTwo(move1, move2, fixed){
        var blockIds3 = testOrderBlocks([move1.id, move2.id]);
        var nonexchangeable = _.map(blockIds3, function(bi){
            return bi[fixed.index] === fixed.id;
        });
        var moveables = _.map(blockIds3, function(bi){
            return bi[move1.index] === move1.id;
        });
        strictEqual(_.compact(nonexchangeable).length, nonexchangeable.length, 'nonexchangeable block should always stay in place');
        notEqual(_.compact(moveables).length, moveables.length, 'exchangeable block should not always stay in place');
    }

    moveTwo({id: 'b1', index: 0}, {id: 'b2', index: 1}, {id: 'b5', index: 2});
    moveTwo({id: 'b1', index: 0}, {id: 'b5', index: 2}, {id: 'b2', index: 1});

    var allmoved = testOrderBlocks(['b1', 'b2', 'b5']);
    var firsts = _.map(allmoved, function(bi){return bi[0];});
    ok(_.isEmpty(_.difference(firsts, ['b1', 'b2', 'b5'])), "every block should appear first sometimes when they're all exchangeable");
});

// try when I understand form
// test("conditionally run blocks", function(){});
