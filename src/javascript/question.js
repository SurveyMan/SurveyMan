//TODO record time of presentation, time of clicking next, time of playing sound/video
//TODO compose questions from text and resources
//TODO correct answer can't be id for text box
var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />
function record(text) {
    $(FORM).append(text);
}

var Page = (function () {
    function Page(block) {
        this.block = block;
    }
    Page.prototype.advance = function () {
    };

    Page.prototype.disableNext = function () {
        $(CONTINUE).prop({ disabled: true });
        if (this.isLast) {
            $(BREAKOFF).show();
        }
    };

    Page.prototype.enableNext = function () {
        $(CONTINUE).prop({ disabled: false });
        if (this.isLast) {
            $(BREAKOFF).hide();
        }
    };

    Page.prototype.nextToSubmit = function () {
        $(CONTINUE).attr({ type: "submit", value: "Submit", form: "surveyman" });
        $(CONTINUE).off('click');
    };

    Page.prototype.display = function () {
        var _this = this;
        if (this.isLast) {
            this.nextToSubmit();
        } else {
            $(CONTINUE).off('click').click(function (m) {
                _this.advance();
            });
        }
        this.disableNext();
        $(OPTIONS).empty();
        $(PAGE).empty().append(this.text);
    };

    Page.prototype.record = function () {
        $(FORM).append(this.id);
    };
    Page.dropdownThreshold = 7;
    return Page;
})();

var Question = (function (_super) {
    __extends(Question, _super);
    function Question(jsonQuestion, block) {
        var _this = this;
        _super.call(this, block);
        var jQuestion = _.defaults(jsonQuestion, { ordered: false, exclusive: true, freetext: false, answer: null, condition: null });
        this.id = jQuestion.id;
        this.text = jQuestion.text;
        this.condition = jQuestion.condition;
        this.ordered = jQuestion.ordered;
        this.exclusive = jQuestion.exclusive;
        this.freetext = jQuestion.freetext;
        if (jQuestion.answer) {
            this.answer = new Statement({ text: jQuestion.answer.text, id: jQuestion.answer.id }, block);
        }
        this.options = _.map(jQuestion.options, function (o) {
            if (jQuestion.options.length > Page.dropdownThreshold) {
                return new DropDownOption(o, _this, _this.exclusive);
            } else if (_this.freetext) {
                return new TextOption(o, _this);
            } else if (_this.exclusive) {
                return new RadioOption(o, _this);
            } else {
                return new CheckOption(o, _this);
            }
        });
        this.orderOptions();
    }
    Question.prototype.display = function () {
        _super.prototype.display.call(this);
        _.each(this.options, function (o) {
            o.display();
        });
    };

    Question.prototype.advance = function () {
        var _this = this;
        record(this.id);
        var selected = _.filter(this.options, function (o) {
            return o.selected();
        });
        _.each(selected, function (s) {
            record(s.getResponse());
        });

        var optAnswers = _.compact(_.map(selected, function (o) {
            return o.getAnswer();
        }));
        if (!_.isEmpty(optAnswers)) {
            _.each(optAnswers, function (ans) {
                var optAnswer = new Statement(ans, _this.block);
                optAnswer.display();
            });
        } else if (this.answer) {
            this.answer.display();
        } else {
            this.block.advance();
        }
    };

    Question.prototype.orderOptions = function () {
        if (this.ordered) {
            if (Math.random() > 0.5) {
                this.options = this.options.reverse();
            }
        } else {
            this.options = _.shuffle(this.options);
        }
    };
    return Question;
})(Page);

var Statement = (function (_super) {
    __extends(Statement, _super);
    function Statement(jsonStatement, block) {
        _super.call(this, block);
        var jStatement = _.defaults(jsonStatement, { condition: null });
        this.text = jsonStatement.text;
        this.id = jsonStatement.id;
        this.condition = jsonStatement.condition;
    }
    Statement.prototype.display = function () {
        var _this = this;
        // super.display();
        if (this.isLast) {
            this.nextToSubmit();
        } else {
            $(CONTINUE).off('click').click(function (m) {
                _this.advance();
            });
        }
        this.disableNext();
        $(OPTIONS).empty();
        $(PAGE).empty().append(this.text);

        setTimeout(3000); //wait 3 seconds before enabling next
        this.enableNext();
    };

    Statement.prototype.advance = function () {
        record(this.id);
        this.block.advance();
    };
    return Statement;
})(Page);
