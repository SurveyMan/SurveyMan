/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />
var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
function record(pageRecord) {
    var data = JSON.parse($(FORM).val());
    data.responses.push(pageRecord);
    $(FORM).val(JSON.stringify(data));
}

var Page = (function () {
    function Page(jsonPage, block) {
        this.block = block;
        jsonPage = _.defaults(jsonPage, { condition: null, resources: null });
        this.id = jsonPage.id;
        this.text = jsonPage.text;
        this.condition = jsonPage.condition;
        this.record = { page: this.id };
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
        // $(CONTINUE).submit(); //TODO
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
    Page.dropdownThreshold = 7;
    return Page;
})();

var Question = (function (_super) {
    __extends(Question, _super);
    function Question(jsonQuestion, block) {
        var _this = this;
        _super.call(this, jsonQuestion, block);
        var jQuestion = _.defaults(jsonQuestion, { ordered: false, exclusive: true, freetext: false });
        this.ordered = jQuestion.ordered;
        this.exclusive = jQuestion.exclusive;
        this.freetext = jQuestion.freetext;
        if (jQuestion.answer) {
            this.answer = new Statement({ text: jQuestion.answer, id: 'ans_' + this.id }, block);
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
        this.record['startTime'] = new Date().getTime();
    };

    Question.prototype.advance = function () {
        this.record['endTime'] = new Date().getTime();

        var selected = _.filter(this.options, function (o) {
            return o.selected();
        });

        // answers that should be displayed to the respondent
        var optAnswers = _.compact(_.map(selected, function (o) {
            return o.getAnswer();
        }));

        this.recordResponses(selected);
        this.recordCorrect(selected);
        record(this.record);

        if (!_.isEmpty(optAnswers) && this.exclusive) {
            optAnswers[0].display();
        } else if (this.answer) {
            this.answer.display();
        } else {
            this.block.advance();
        }
    };

    Question.prototype.recordResponses = function (selected) {
        // ids of selections and value of text
        var responses = _.map(selected, function (s) {
            return s.getResponse();
        });
        this.record['selected'] = responses;
    };

    Question.prototype.recordCorrect = function (selected) {
        this.record['correct'] = _.map(selected, function (s) {
            return s.isCorrect();
        });
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
    function Statement() {
        _super.apply(this, arguments);
    }
    Statement.prototype.display = function () {
        var _this = this;
        _super.prototype.display.call(this);
        this.record['startTime'] = new Date().getTime(); //TODO is this the desired behavior? should there be a delay?
        setTimeout(function () {
            _this.enableNext();
        }, 2000);
    };

    Statement.prototype.advance = function () {
        this.record['endTime'] = new Date().getTime();
        record(this.record);
        this.block.advance();
    };
    return Statement;
})(Page);
