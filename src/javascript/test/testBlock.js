
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
    var firstconditions = _.map(_.range(10), function(){
        var b = new InnerBlock(jsonb);
        var conditions = _.pluck(b.contents, "condition");
        return conditions[0];
    });
    strictEqual(_.unique(firstconditions).length, 3, "any condition should be able to end up first");

    jsonb.pseudorandomize = true;
    var b2 = new InnerBlock(jsonb, {});
    var conditions2 = _.pluck(b2.contents, "condition");
    var firstconditions2 = _.map(_.range(10), function(){
        var b = new InnerBlock(jsonb);
        var conditions = _.pluck(b.contents, "condition");
        return conditions[0];
    });

    strictEqual(_.unique(firstconditions2).length, 3, "any condition should be able to end up first");
    var adjacentconditions = _.zip(b2.contents, _.rest(b2.contents));
    var clashes = _.filter(adjacentconditions, function(pair){return pair[0] === pair[1];});
    strictEqual(clashes.length, 0, "no two adjacent conditions should be the same when pseudorandomized");
});

test("page calling advance", function(){
    setup();
    function clickNext(){$(":button").trigger("click");}
    function CustomError( message ) {
        this.message = message;
      }

    CustomError.prototype.toString = function() {
        return this.message;
      };

    var pgs = [{text:"page1", id:"p1"},
        {text:"page2", id:"p2", freetext: true, options:[{id:"o2"}]},
        {text:"page3", id:'p3', freetext: true, options:[{id:"o1"}], answer:"good job"}];
    var b = new InnerBlock({id:"b1", pages: pgs}, {advance: function(){throw new CustomError("I advanced");}});
    b.advance();

    // statement displays
    strictEqual($("p.question").text, "page1", "statement should display");
    strictEqual($(":button").length, 1, "should be a next button");
    setTimeout(clickNext, 4000);
    clickNext();

    // question displays
    strictEqual($("p.question *").length, 1, "question has displayed");
    strictEqual($("#o2").length, 1);
    $("#o2").val("something");
    strictEqual($("#o2").val(), "something");
    $(":input").trigger("keyup");
    clickNext();
    // throws(clickNext, CustomError, "does statement advance call block advance?");
    // b.advance();
    // $(":button").trigger("click");
    // b.advance();
    // $(":button").trigger("click");
    // b.advance();
    // $(":button").trigger("click");
});

