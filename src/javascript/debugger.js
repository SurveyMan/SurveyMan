var sm = {},
    corrs = {},
    bkoffs = {},
    variants = {},
    order = {},
    responses = {},
    surveyData = [],
    resultsData = [],
    staticCurrentSurveyId = "staticCurrentSurvey",
    dynamicCurrentSurveyId = "dynamicCurrentSurvey";
    staticBtnCurrentSurveyId = "staticBtnCurrentSurvey",
    dynamicBtnCurrentSurveyId = "dynamicBtnCurrentSurvey",
    targets = ["overview", "static", "dynamic"],
    margin = { top : 20, right : 0, bottom : 0, left: 15},
    width = 960,
    height = width;

var toggle_task         =   function (target) {

        console.log(target);

        $.get(target + ".html"
            , function (data) {
                $("#content").empty();
                $("#content").html(data);
                if (target==="static") {
                    document.getElementById('staticFiles').addEventListener('change', function (evt) { handleFileSelect(evt, true); }, false);
                } else if (target==="dynamic") {
                    document.getElementById('dynamicFiles').addEventListener('change', function (evt) { handleFileSelect(evt, false);}, false);
                    document.getElementById('dynamicBtnResults').addEventListener('change', function (evt) { handleFileSelect(evt, false, true);}, false);
                }
            }
        );

        },
    display_correlations = function () {
        return makeHeatmap(corrs, sm);
    },
    display_breakoff    = function () {
        return makeBarchart(bkoffs, sm);
    },
    display_variants    = function () {
        return makeVariantDisplay(variants, sm);
    },
    display_order       = function () {
        return makeOrderDisplay(order, sm);
    },
    display_scores      = function () {
        return makeScoresDisplay(scores, sm);
    },
    analysis            =   function (reportType, csv, local, data) {

            var report  =   reportType ? "static" : "dynamic",
                obj     =   {"report" : report,
                             "survey" : csv,
                             "local" : (local || false),
                             "surveyData" : (data ? data["surveyData"] : ""),
                             "resultsData" : (data ? data["resultsData"] : "")
                             };
            console.log(obj);
            console.assert($.isPlainObject(obj));

            if (reportType) {
                $.post("", obj, function (s) {
                    console.log(s);
                    $("#" + report + "Data").html("<br/>" + s + "<br/>");
                }).always(function () {
                    $("#staticBtnCurrentSurvey").button("reset");
                });
            } else {
                $.post("", obj, function (s) {
                    console.log(s);
                    var retval = JSON.parse(s);
                    sm = new SurveyMan(retval['sm']);
                    corrs = retval['corrs'];
                    bkoffs = retval['bkoffs'];
                    variants = retval['variants'];
                    order = retval['order'];
                    responses = retval['responses'];
                    $("#correlation").removeClass("disabled");
                    $("#breakoff").removeClass("disabled");
                    $("#variants").removeClass("disabled");
                    $("#order").removeClass("disabled");
                    $("#scores").removeClass("disabled");
                }).always(function () {
                    $("#dynamicBtnCurrentSurvey").button("reset");
                }
                ); // produces the csvs
            }

        },
    updateCurrentSurvey = function(displayText, filename, reportType, local, results, cbk) {

            var report              =   reportType ? "static" : "dynamic",
                currentSurvey       =   reportType ? $("#"+staticCurrentSurveyId) : $("#"+dynamicCurrentSurveyId),
                btnCurrentSurvey    =   reportType ? $("#"+staticBtnCurrentSurveyId) : $("#"+dynamicBtnCurrentSurveyId);

            console.log(arguments);

            $(btnCurrentSurvey).html(displayText);
            $(btnCurrentSurvey).unbind("click");
            $(btnCurrentSurvey).click((function (cbk, btnCurrentSurvey) {
                return function() {
                        cbk();
                        $(btnCurrentSurvey).button('loading');
                     };
                })(cbk, btnCurrentSurvey));

            if (!local) {
                btnCurrentSurvey.removeAttr("disabled");
                $("#dynamicBtnResults").attr("disabled", "disabled");
            }

            if (results) {
                $("#dynamicBtnCurrentSurvey").html($("#dynamicBtnCurrentSurvey").html()+"<br/>"+displayText);
                $("#dynamicBtnCurrentSurvey").removeAttr("disabled");
            } else {
                $("#dynamicCurrentResults").hide();
                $(currentSurvey).show();
                if (reportType){
                    $("#staticData").empty();
                } else {
                    $("#heatmap").empty();
                    $("#questionCloseup").empty();
                }

            }

        },
    handleFileSelect    = function (evt, reportType, results) {

            if (!(window.File || window.FileReader || window.FileList || window.Blob)) {
                alert("Cannot upload files! The File APIs are not fully supported on your browser");
            } else {

                var files = evt.target.files; // FileList object

                // files is a FileList of File objects. List some properties.
                var display = [];
                console.assert(files.length === 1);
                f = files[0];
                display.push('<strong>', escape(f.name), '</strong> (', f.type || 'n/a', ') - ',
                              f.size, ' bytes, last modified: ',
                              f.lastModifiedDate ? f.lastModifiedDate.toLocaleDateString() : 'n/a',
                              '');

                //read the data from the file into memory
                var r = new FileReader();

                r.onload = (function (selectedFile, display, reportType, results) {
                    return function (evt) {
                        //return the string data
                        var data = evt.target.result,
                            cbk  = function() {
                                       analysis(reportType, selectedFile.name, true, {"surveyData" : surveyData, "resultsData" : resultsData});
                                    };
                        if (results) {
                            // then it must be dynamic analyses
                            resultsData = data;
                        } else {
                            surveyData = data;
                        }
                        console.log(cbk);
                        updateCurrentSurvey(display, f.name, reportType, true, results, cbk);
                    }
                })(f, display.join(''), reportType, results);

                r.readAsText(f,"UTF-8");
            }
        },
    sendLocalSurvey   =   function(){
        console.assert($("#dynamicBtnCurrentResults").attr("disabled"));
        };


var NaNOrZero = function (_d) {
                    if (isNaN(_d.corr))
                        return 0;
                    else return _d.corr;
                };

var getQuestionHTML = function(q, ct) {
    var div = document.createElement("div");
//    div.id = q.id;
//    $(div).attr({"class" : "col-md-3"});
//    $(div).css("background", "#FFFAFA");
    $(div).css("margin-top" , "10px");
    $(div).css("padding-right", "10px");
    //$(div).css("border", "solid");
    $(div).append("<p>" + q.qtext + "</p>");
    $(div).append(sm.getOptionHTML(q));
    return div;
};

var getResponseCounts = function(q) {
    // return a map of options to counts
    var jsonizedResponses = _.map(responses, function (r) { return JSON.parse(r.response.responses); }),
        responsesForQ = _.map(jsonizedResponses, function (sr) { return _.filter(sr, function (r) { return r.q===q.id;})})
//    var retval = {};
//    for (var i = 0; i < responsesForQ.length ; i++) {
//        if (responsesForQ[i].length > 0 && responsesForQ[i][0].hasOwnProperty("opts")) {
//            var k = responsesForQ[i][0].opts[0];
//            console.log(k);
//            if (_.has(retval, k))
//                retval[k] = retval[k] + 1;
//            else retval[k] = 1;
//        }
//    }
//    console.log("retval", retval);
//    return retval;
    return _.filter(responsesForQ, function (rfq) { return rfq.length > 0 && rfq[0].hasOwnProperty("opts"); });
};

var makeResponseChart = function (q, responseMap, targetDiv) {

    console.log("responseMap", responseMap);
    console.log($(targetDiv).attr("width"));
    console.log($(targetDiv).width());

    var indexLookup = _.map(q.options, function (o) { return o.id; }),
        svgWidth = 300,
        barWidth = svgWidth / q.options.length - 5,
        ctHeight = 10,
        axisThickness = 1,
        color = _.uniq(_.map(_.range(q.options.length), function (foo) { return "hsl(" + ((360 / q.options.length) * foo) + ",100%,50%)"; }))
        data = _.flatten(_.map(q.options, function (option) {
                                                return _.map(_.range(q.options.length), function (i) {
                                                                                            return {
                                                                                                id : option.id,
                                                                                                text : option.otext,
                                                                                                index : i,
                                                                                                ct : _.filter(responseMap, function (m) {
                                                                                                                            var opt = m[0].opts[0];
                                                                                                                            return opt.o===option.id && opt.oindex===i;}).length
                                                                                            };
                                                                                        });
                                           })),
        svgHeight = 300; //_.reduce(_.map(data, function (d) { return d.ct }), function (a, b) { return a + b; }, 0) * ctHeight;



    console.log("data", data);

    var svg = d3.select(targetDiv).append("svg")
        .attr("width", svgWidth)
        .attr("height", svgHeight)
        .append("g")
        .attr("transform", "translate("+ margin.left + "," + margin.top + ")");

    var bars = svg.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d, i) { return _.indexOf(indexLookup, d.id) * barWidth; })
        .attr("y", function (d, i) {
                        return _.reduce(_.filter(data, function (_d) { return d.id === _d.id && _d.index < d.index; })
                                              , function (n, o) { return n + o.ct; }
                                              , 0) * ctHeight;
                   })
        .attr("height", function (d, i) { return d.ct * ctHeight; })
        .attr("width", barWidth)
        .attr("fill", function(d, i) { return color[d.index]; })
        .attr("stroke", "black")
        .attr("stroke-width", 1);

    bars.append("title")
        .text(function (d) { return d.text + " (index: " + d.index + ", count: " + d.ct + ")"; });

    var xAxis = svg.append("line")
        .attr("x1", 0)
        .attr("y1", 0)
        .attr("x2", svgWidth)
        .attr("y2", 0)
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black");

    var yAxis = svg.append("line")
        .attr("x1", 0)
        .attr("x2", 0)
        .attr("y1", 0)
        .attr("y2", svgHeight)
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black");



};