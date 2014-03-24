//TODO other radio/check with text box
var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />
var ResponseOption = (function () {
    function ResponseOption(jsonOption, question) {
        this.question = question;
        jsonOption = _.defaults(jsonOption, { answer: null, correct: null });
        this.id = jsonOption.id;
        this.text = jsonOption.text;
        this.answer = jsonOption.answer;
        this.correct = jsonOption.correct; // has to be specified as false in the input for radio/check/dropdown if it should count as wrong
    }
    ResponseOption.prototype.display = function () {
    };

    ResponseOption.prototype.getResponse = function () {
        return this.id;
    };

    ResponseOption.prototype.onChange = function () {
        if ($(OPTIONS + " :checked").length !== 0) {
            this.question.enableNext();
        } else {
            this.question.disableNext();
        }
    };

    ResponseOption.prototype.selected = function () {
        var selectedIDs = _.pluck($(OPTIONS + " :checked"), "id");
        return _.contains(selectedIDs, this.id);
    };

    ResponseOption.prototype.getAnswer = function () {
        if (this.answer) {
            return new Statement({ text: this.answer, id: 'ans_' + this.id }, this.question.block);
        } else {
            return null;
        }
    };

    ResponseOption.prototype.isCorrect = function () {
        return this.correct;
    };
    return ResponseOption;
})();

var RadioOption = (function (_super) {
    __extends(RadioOption, _super);
    function RadioOption() {
        _super.apply(this, arguments);
    }
    RadioOption.prototype.display = function () {
        var _this = this;
        var label = document.createElement("label");
        $(label).attr("for", this.id);
        $(label).append(this.text);

        var input = document.createElement("input");
        $(input).attr({ type: "radio", id: this.id, name: this.question.id });
        $(input).change(function (m) {
            _this.onChange();
        });

        $(OPTIONS).append(label);
        $(OPTIONS).append(input);
    };
    return RadioOption;
})(ResponseOption);

var CheckOption = (function (_super) {
    __extends(CheckOption, _super);
    function CheckOption() {
        _super.apply(this, arguments);
    }
    CheckOption.prototype.display = function () {
        var _this = this;
        var label = document.createElement("label");
        $(label).attr("for", this.id);
        $(label).append(this.text);

        var input = document.createElement("input");
        $(input).attr({ type: "checkbox", id: this.id, name: this.question.id });
        $(input).change(function (m) {
            _this.onChange();
        });

        $(OPTIONS).append(label);
        $(OPTIONS).append(input);
    };
    return CheckOption;
})(ResponseOption);

var TextOption = (function (_super) {
    __extends(TextOption, _super);
    function TextOption(jsonOption, block) {
        jsonOption = _.defaults(jsonOption, { text: "", regex: null });
        _super.call(this, jsonOption, block);
        if (jsonOption.regex) {
            this.regex = new RegExp(jsonOption.regex);
        }
    }
    TextOption.prototype.display = function () {
        var _this = this;
        var input = document.createElement("input");
        $(input).attr({ type: "text", id: this.id, name: this.question.id });
        $(input).keyup(function (m) {
            _this.onChange();
        });

        $(OPTIONS).append(input);
    };

    TextOption.prototype.getResponse = function () {
        // return $("#"+this.id).val(); // TODO sometimes doesn't work and I don't know why
        return $(OPTIONS + " input").val();
    };

    TextOption.prototype.onChange = function () {
        if (this.getResponse()) {
            this.question.enableNext();
        } else {
            this.question.disableNext();
        }
    };

    TextOption.prototype.selected = function () {
        return this.getResponse().length > 0;
    };

    TextOption.prototype.isCorrect = function () {
        if (this.regex) {
            return Boolean(this.getResponse().match(this.regex));
        } else {
            return null;
        }
    };
    return TextOption;
})(ResponseOption);

var DropDownOption = (function (_super) {
    __extends(DropDownOption, _super);
    function DropDownOption(jsonOption, block, exclusive) {
        _super.call(this, jsonOption, block);
        this.exclusive = exclusive;
    }
    DropDownOption.prototype.display = function () {
        var _this = this;
        //if select element exists, append to it, otherwise create it first
        if ($("select").length === 0) {
            var select = document.createElement("select");
            if (!this.exclusive) {
                $(select).attr({ multiple: "multiple", name: this.question.id });
            }
            $(select).change(function (m) {
                _this.onChange();
            });
            $(OPTIONS).append(select);
        }
        var option = document.createElement("option");
        $(option).attr("id", this.id);
        $(option).append(this.text);
        $("select").append(option);
    };
    return DropDownOption;
})(ResponseOption);
