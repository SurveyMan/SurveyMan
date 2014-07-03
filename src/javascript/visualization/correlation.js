var NaNOrZero = function (_d) {
                    if (isNaN(_d.corr))
                        return 0;
                    else return _d.corr;
                };

var getQuestionHTML = function(q, ct, sm) {
    var div = document.createElement("div");
    $(div).attr({"class" : "col-md-3"});
//    $(div).css("background", "#FFFAFA");
    $(div).css("margin-top" , "10px");
    $(div).css("padding-right", "10px");
    //$(div).css("border", "solid");
    $(div).append("<p>" + q.qtext + "</p>");
    $(div).append(sm.getOptionHTML(q));
    return div;
};

var getCorrelationData = function (d, sm) {
    var div = document.createElement("div");
    $(div).append("<p><b>#/Responses</b>:&nbsp;"+d.ct1
    +"&nbsp;&nbsp;<b>Coeff</b>:&nbsp;"+d.coeff
    +"&nbsp;&nbsp;<b>Val</b>:&nbsp;"+d.corr
    +"</p>");
    $(div).css("margin-top" , "10px");
    return div;
};

var zoomCorrelation = function(d, sm) {

    // hide heatmap
    $("#heatmap").hide();
    $("#questionCloseup").empty();
    $("#questionCloseup").append(getCorrelationData(d, sm));
    $("#questionCloseup").append(getQuestionHTML(d.q1, d.ct1, sm));
    $("#questionCloseup").append(getQuestionHTML(d.q2, d.ct2, sm));
    $("#questionCloseup").append("<div class=\"col-md-10\"><button class=\"btn\" onclick='$(\"#questionCloseup\").empty(); $(\"#heatmap\").show();'>Return to Heatmap</button></div>");
};

var makeHeatmap = function (_jsonCorrs, sm) {

    // adapted from http://bl.ocks.org/tjdecke/5558084
    // _jsonCorrs is a list of json objects

    console.log($(""))

    if ($("#heatmap").children().length != 0)
        return;


    var margin = { top: 15, right: 0, bottom: 0, left: 5 },
        width = 960 - margin.left - margin.right,
        height = width,
        questions = _.map(_.filter(sm.survey.questions, function(_q) {
                                                            return !_q.freetext;
                                                        }), function (_q) {
                                                            return _q.id;
                                                            }),
        gridSize = Math.floor(width / questions.length),
        legendElementWidth = width/9,
        legendElementHeight = gridSize*2,
        buckets = 9,
        //colors = ["#ffffd9","#edf8b1","#c7e9b4","#7fcdbb","#41b6c4","#1d91c0","#225ea8","#253494","#081d58"],
        colors = ['rgb(178,24,43)','rgb(214,96,77)','rgb(244,165,130)','rgb(253,219,199)','rgb(247,247,247)','rgb(209,229,240)','rgb(146,197,222)','rgb(67,147,195)','rgb(33,102,172)'],
        threshholds = [-0.8, -0.6, -0.4, -0.2, 0, 0.2, 0.4, 0.6, 0.8],
        legendLabels = ["&lt; -0.77", "-0.77 &mdash; -0.55", "-0.55 &mdash; -0.33"
                        , "-0.33 &mdash; -0.11", "-0.11 &mdash; 0.11", "0.11 &mdash; 0.33"
                        , "0.33 &mdash; 0.55", "0.55 &mdash; 0.77", "&gt; 0.77"],
        processCorrs = function (_d) {
                // this gets called on
                return { q1 : sm.getQuestionById(_d.q1),
                               ct1 : +_d.ct1,
                               q2 : sm.getQuestionById(_d.q2),
                               ct2 : +_d.ct2,
                               corr : parseFloat(_d.val),
                               coeff : _d.coeff,
                               expected : JSON.parse(_d.expected)
                               };
            };

    console.assert(buckets===colors.length);
    console.assert(buckets===threshholds.length);
    console.assert(buckets===legendLabels.length);

    var colorScale = d3.scale.quantile()
                          .domain([-1.0, 1.0])
                          .range(colors);

    for (var i = 0 ; i < colors.length ; i++)
        console.log(colorScale.invertExtent(colors[i]));

    var svg = d3.select("#heatmap").append("svg")
                              .attr("width", width + margin.left + margin.right)
                              .attr("height", height + margin.top + margin.bottom)
                              .append("g")
                              .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var heatMap = svg.selectAll("#heatmap")
                    .data(_.map(corrs, processCorrs))
                    .enter()
                    .append("rect")
                    .attr("x", function(d) { return _.indexOf(questions, d.q1.id) * gridSize; })
                    .attr("y", function(d) { return _.indexOf(questions, d.q2.id) * gridSize; })
                    .attr("rx", 4)
                    .attr("ry", 4)
                    .attr("class", "hour bordered")
                    .attr("width", gridSize)
                    .attr("height", gridSize)
                    .style("fill", colors[0]);

    console.log(heatMap.selectAll("rect").length);

    heatMap.attr("cursor", "pointer")
        .on("click", function (d, i) {
                zoomCorrelation(d, sm);
                return;
            });

    heatMap.transition().duration(1000)
      .style("fill", function(d) { return colorScale(d.corr); });

    heatMap.append("title").text(function(d) { return "(" + d.q1.id + ", " + d.q2.id + ")" + " : " + d.corr; });


    var legend = svg.selectAll(".legend")
        .data(threshholds)
        .enter().append("g")
        .attr("class", "legend");

    legend.append("rect")
      .attr("x", function(d) { return legendElementWidth * _.indexOf(threshholds, d); })
      .attr("y", sm.survey.questions.length*gridSize)
      .attr("width", legendElementWidth)
      .attr("height", legendElementHeight)
      .style("fill", function(d) { return colors[_.indexOf(threshholds, d)]; });

    legend.append("text")
      .html(function (d) { return legendLabels[_.indexOf(threshholds, d)]; })
      .attr("font-weight", "bold")
      .attr("color", "black")
      .attr("font-size", "small")
      .attr("x", function(d) { return (legendElementWidth * _.indexOf(threshholds, d)) + 10; })
      .attr("y", questions.length*gridSize + 15);

    var expected_show = d3.select("#unexpectedCorrs");
    var expected_hide = d3.select("#allCorrs");
    var only_expected = d3.select("#failedCorrs");

    expected_show.on("click", function () {
                             heatMap.transition().duration(1000)
                                 .style("fill", function (d) {
                                     if (d.expected)
                                         return 'rgb(255,255,255)';
                                     else return colorScale(d.corr);
                                     });
                             expected_show.style("display", "none");
                             expected_hide.style("display", null);
                         });

    only_expected.on("click", function () {
                             heatMap.transition().duration(1000)
                                 .style("fill", function (d) {
                                     if (d.expected && -0.5 < d.corr && d.corr < 0.5 )
                                         return colorScale(d.corr);
                                     else return 'rgb(255,255,255)';
                                     });
                             only_expected.style("display", "none");
                             expected_hide.style("display", null);
                         });

    expected_hide.on("click", function () {
                             heatMap.transition().duration(1000)
                                 .style("fill", function (d) { return colorScale(d.corr); });
                             expected_show.style("display", null);
                             expected_hide.style("display", "none");
                             only_expected.style("display", null);
                        });


    };