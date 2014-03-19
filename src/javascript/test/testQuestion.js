test("statement initialization", function(){
    var jsonq = {"text": "Do I pass?", id: "q1"};
    var q = new Statement(jsonq, {});
    strictEqual(q.text, "Do I pass?", "statement text not set properly");
    strictEqual(q.isLast, undefined, "statement.isLast not set properly");
    strictEqual(q.id, "q1", "statment id not set properly");
    strictEqual(q.condition, null, "statement condition default not working");
    q.isLast = true;
    strictEqual(q.isLast, true, "statement.isLast not set properly");
    jsonq.condition = "a";
    var q2 = new Statement(jsonq, {});
    strictEqual(q2.condition, "a");
});

test("question initialization", function(){
    var jsonq = {"text": "some text", id: "q2", ordered: false, exclusive: false,
        options: [{text: "option A", id: "o1"},
            {text: "option B", id: "o2", branchTo: 3}]};
    var q = new Question(jsonq, {});
    strictEqual(q.text, "some text", "question text not set properly");
    strictEqual(q.id, "q2", "question id not set properly");
    strictEqual(q.ordered, false, "question.ordered not set properly");
    strictEqual(q.exclusive, false, "question.exclusive not overcoming default");
    strictEqual(q.freetext, false, "question.freetext default not working");
    strictEqual(q.options.length, 2, "question.options not set properly");
    ok(q.options[0].question instanceof Question, "question not sending right 'this' to options");
});


test("checkbox options", function(){
    // exclusive is false and number of options is low so should be checkboxes
    var jsonq = {"text": "Do I pass?", "ordered": true, "freetext": false, "exclusive": false, "options": [{"text": "option A"}, {"text": "option B", "branchTo": 1}]};
    var q = new Question(jsonq);
    strictEqual(q.options.length, 2, "options not created properly");
    ok(q.options[0] instanceof CheckOption, "CheckOption should have been created");
});

test("radio options", function(){
    // exclusive is false and number of options is low so should be checkboxes
    var jsonq = {id: "12", "text": "Do I pass?", "ordered": true, exclusive: true, "freetext": false, "options": [{"text": "option A"}, {"text": "option B", "branchTo": 1}]};
    var q = new Question(jsonq, true, false, {}, {});
    strictEqual(q.options.length, 2);
    ok(q.options[0] instanceof RadioOption, "RadioOption should have been created");
    ok(q.options[0].question instanceof Question, "question sending option wrong 'this'");
});

test("dropdown options", function(){
    var os = _.map(_.range(8), function(i){return {text: i.toString(), id: i};});
    var q = new Question({text: "q", id: 10, exclusive: true, options: os}, {});
    var q2 = new Question({text: "q2", id: 11, exclusive: false, options: os}, {});
    ok(q.options[0] instanceof DropDownOption, "DropDownOption should have been created");
    strictEqual(q.options[0].exclusive, true, "DropDownOption doesn't know exclusivity");
    ok(q2.options[0] instanceof DropDownOption, "DropDownOption should have been created even though exclusive is false");
    strictEqual(q2.options[0].exclusive, false, "DropDownOption doesn't know exclusivity");
});

test("text option", function(){
    var q = new Question( {text: "q", id: 1, freetext: true, options:[{id: 2, text: ''}]}, {} );
    ok(q.options[0] instanceof TextOption);
});

test("question with answer", function(){
    var q = new Question({id: 2, "text": "here's the question", answer: {text: "here's the answer", id: "o1"},
                         options: [{text: "option A", id:"o1"}, {text: "option B", id:"o2"}]}, {});
    strictEqual(q.options.length, 2);
    ok(q.answer instanceof Statement, "Statement not created from answer");
    strictEqual(q.answer.text, "here's the answer", "answer text not set properly");
});

test("question with options with answers", function(){

});

test("option ordering", function(){
    var scale = {id: 10, "text": "here's the question", ordered: true,
                         options: [{id: 1, text: "option A"}, {id: 2, text: "option B"}, {id: 3, text: "option C"}]};
    var bag = {id: 11, "text": "here's the question",
                         options: [{id: 4, text: "option A"}, {id: 5, text: "option B"}, {id: 6, text: "option C"}]};
    var scaleResults = [];
    var bagResults = [];
    for (var i = 0; i < 30; i++){
        var s = new Question(scale, {});
        scaleResults.push(s.options[1].id === 2);
        var b = new Question(bag, {});
        bagResults.push(b.options[1].id === 5);
    }
    ok(_.every(scaleResults), "middle option should always be in the middle when ordered options are reordered");
    ok(_.some(bagResults), "middle option should sometimes be in the middle when unordered options are shuffled");
    ok(_.contains(bagResults, false), "middle option should sometimes not be in the middle when unordered options are shuffled");
});

function setup(){
    var $fixture = $( "#qunit-fixture" );
    $fixture.append("<div id='question'><p class='question'></p><p class='answer'></p></div><div class='navigation'></div><div class='breakoff'></div>");
    var nextButton = document.createElement("input");
    $(nextButton).attr({type: "button", id: "continue", value: "Next"});
    $("div.navigation").append(nextButton);
}

test("statement display", function(){
    setup();
    strictEqual($("div.navigation").length, 1, "setup didn't work");
    var jsons = {"text": "Do I pass?", id: "s1"};
    var s = new Statement(jsons, {});
    s.display();
    strictEqual($("p.question").text(), "Do I pass?", "statement text not appended properly");
    strictEqual($("p.answer").text(), '', "statement shouldn't put anything in answer paragraph");
    strictEqual($(":button").length, 1, "There should be one Next button");
    s.isLast = true;
    s.display();
    strictEqual($("p.question").text(), "Do I pass?", "statement text not appended properly");
    strictEqual($("p.answer").html().length, 0, "statement shouldn't put anything in answer paragraph");
    strictEqual($(":button[value='Next']").length, 0, "Next button shouldn't display");
    strictEqual($(":submit").length, 1, "Submit button not showing");
});

test("question display with radios", function(){
    setup();
    var opts = [{text: "A", id: "o1"}, {text: "B", id: "o2"}];
    var q = new Question({text: "Do I pass?", id: "q1", options: opts}, {});
    q.display();
    strictEqual($("p.question").text(), "Do I pass?", "is question text accurate (not duplicated)?");
    strictEqual($("p.answer :input").length, 2, "did option inputs get appended?");
    strictEqual($("p.answer *").length, 4, "did option inputs and labels get appended?");
    strictEqual($(":button").length, 1, "should be a next button");
    strictEqual($(":button").prop("disabled"), true, "next button should be disabled");
    var id1 = q.options[0].id;
    var id2 = q.options[1].id;
    $(":input[id='"+id1+"']").prop("checked", true);
    $(":input[id='"+id1+"']").trigger("change");
    strictEqual($(":button").prop("disabled"), false, "next button should be enabled");
    q.isLast = true;
    q.display();
    strictEqual($("p.question").text(), "Do I pass?", "is question text accurate?");
    strictEqual($("p.answer :input").length, 2, "did option inputs get appended?");
    strictEqual($("p.answer *").length, 4, "did option inputs and labels get appended?");
    strictEqual($(":button").length, 0, "should not be a next button");
    strictEqual($(":submit").length, 1, "should be a submit button");
    strictEqual($(":submit").prop("disabled"), true, "submit button should be disabled");
    $(":input[id='"+id1+"']").prop("checked", true);
    $(":input[id='"+id1+"']").trigger("change");
    strictEqual($(":submit").prop("disabled"), false, "submit button should be enabled");
    strictEqual(q.options[0].selected(), true, "does option know it's selected?");
    strictEqual(q.options[1].selected(), false, "does option know it's not selected?");
});

test("question display with checkboxes", function(){
    setup();
    var opts = [{text: "A", id: "o1"}, {text: "B", id: "o2"}];
    var q = new Question({text: "Do I pass?", id: "q1", exclusive: false, options: opts}, {});
    q.display();
    strictEqual($("p.answer *").length, 4, "did option inputs and labels get appended?");
    strictEqual($(":button").length, 1, "should be a next button");
    strictEqual($(":button").prop("disabled"), true, "next button should be disabled");
    var id1 = q.options[0].id;
    var id2 = q.options[1].id;
    $(":input[id='"+id1+"']").prop("checked", true);
    $(":input[id='"+id1+"']").trigger("change");
    strictEqual($(":button").prop("disabled"), false, "next button should be enabled");
    strictEqual(q.options[0].selected(), true, "does option know it's selected?");
    strictEqual(q.options[1].selected(), false, "does option know it's not selected?");
});

test("question display with dropdown", function(){
    setup();
    //it's important that ids be strings!
    var os = _.map(_.range(8), function(i){return {text: i.toString(), id: i.toString()};});
    var q = new Question({text: "Do I pass?", id: "q1", exclusive: false, options: os}, {});
    q.display();
    strictEqual($("p.answer *").length, 9, "did select and its options get appended?");
    strictEqual($("option").length, 8, "did options get appended?");
    strictEqual($(":button").length, 1, "should be a next button");
    strictEqual($(":button").prop("disabled"), true, "next button should be disabled");
    strictEqual($("option:selected").length, 0);
    var id1 = q.options[0].id;
    var id2 = q.options[1].id;
    $("option[id='"+id1+"']").prop("selected", "selected");
    strictEqual($("option:selected").length, 1);
    $("select").trigger("change");
    strictEqual($(":button").prop("disabled"), false, "next button should be enabled");
    strictEqual(q.options[0].selected(), true, "does option know it's selected?");
    strictEqual(q.options[1].selected(), false, "does option know it's not selected?");
});

test("question display with text", function(){
    setup();
    var opt = [{id: "o1", text: "starter"}];
    var q = new Question({text: "Do I pass?", id: "q1", freetext: true, options: opt}, {});
    q.display();
    strictEqual($("p.answer input").length, 1, "did textbox get appended?");
    strictEqual($(":button").length, 1, "should be a next button");
    strictEqual($(":button").prop("disabled"), true, "next button should be disabled");
    strictEqual($("p.answer input").val(), "", "text should start out blank (not supporting placeholders currently)");
    $("#o1").val("new");
    strictEqual($("p.answer input").val(), "new", "did changing text work?");
    strictEqual($("#o1").val(), "new", "did changing text work?");
    $("#o1").trigger("keyup");
    strictEqual($(":button").prop("disabled"), false, "next button should be enabled");
    strictEqual(q.options[0].selected(), true, "does option know it has text?");
    $("#o1").val("");
    strictEqual(q.options[0].selected(), false, "does option know it doesn't have text?");
});
