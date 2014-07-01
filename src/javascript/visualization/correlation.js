var NaNOrZero = function (_d) {
                    if (isNaN(_d.corr))
                        return 0;
                    else return _d.corr;
                };

var heatmap = function (_jsonCorrs, sm) {

    // adapted from http://bl.ocks.org/tjdecke/5558084
    // _jsonCorrs is a list of json objects

    var margin = { top: 100, right: 0, bottom: 0, left: 50 },
        width = 960 - margin.left - margin.right,
        height = width,
        gridSize = Math.floor(width / sm.survey.questions.length),
        legendElementWidth = gridSize*4,
        buckets = 9,
        //colors = ["#ffffd9","#edf8b1","#c7e9b4","#7fcdbb","#41b6c4","#1d91c0","#225ea8","#253494","#081d58"],
        colors = ['rgb(178,24,43)','rgb(214,96,77)','rgb(244,165,130)','rgb(253,219,199)','rgb(247,247,247)','rgb(209,229,240)','rgb(146,197,222)','rgb(67,147,195)','rgb(33,102,172)'],
        threshholds = [-0.8, -0.6, -0.4, -0.2, 0, 0.2, 0.4, 0.6, 0.8],
        questions = _.map(sm.survey.questions, function (_q) { return _q.id; })
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

    var colorScale = d3.scale.quantile()
                          .domain([-1.0, 1.0])
                          //.domain([0, buckets - 1, d3.max(_data, function (d) { return NaNOrZero(d.corr); })])
                          .range(colors);

    var svg = d3.select("#corrs").append("svg")
                              .attr("width", width + margin.left + margin.right)
                              .attr("height", height + margin.top + margin.bottom)
                              .append("g")
                              .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var xQuestionLabels = svg.selectAll(".qLabel")
                              .data(questions)
                              .enter().append("text")
                              .text(function (d) { return d.id; })
                              .attr("x", 0)
                              .attr("y", function (d) { return _.indexOf(questions, d) * gridSize; })
                              .style("text-anchor", "end")
                              .attr("transform", "translate(-6," + gridSize / 1.5 + ")")
                              .attr("class", function (d, i) { return ((i >= 0 && i <= 4) ? "dayLabel mono axis axis-workweek" : "dayLabel mono axis"); }),
        yQuestionLabels = svg.selectAll(".timeLabel")
                              .data(questions)
                              .enter().append("text")
                              .text(function(d) { return d.id; })
                              .attr("x", function(d) { return _.indexOf(questions, d) * gridSize; })
                              .attr("y", 0)
                              .style("text-anchor", "middle")
                              .attr("transform", "translate(" + gridSize / 2 + ", -6)")
                              .attr("class", function(d, i) { return ((i >= 7 && i <= 16) ? "timeLabel mono axis axis-worktime" : "timeLabel mono axis"); });

    var heatMap = svg.selectAll(".corrs")
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

    heatMap.transition().duration(1000)
      .style("fill", function(d) { return colorScale(d.corr); });

    heatMap.append("title").text(function(d) { return "(" + d.q1.id + ", " + d.q2.id + ")" + " : " + d.corr; });


//    var legend = svg.selectAll(".legend")
//        //.data([0].concat(colorScale.quantiles()), function(d) { return d; })
//        .data(threshholds)
//        .enter().append("g")
//        .attr("class", "legend");
//
//    legend.append("rect")
//      .attr("x", function(d, i) { return legendElementWidth * i; })
//      .attr("y", height)
//      .attr("width", legendElementWidth)
//      .attr("height", gridSize / 2)
//      .style("fill", function(d, i) { return colors[i]; });
//
//    legend.append("text")
//      .attr("class", "mono")
//      .text(function(d) { return "" + d; })
//      .attr("x", function(d, i) { return legendElementWidth * i; })
//      .attr("y", height + gridSize);
//
//    var expected_show = d3.select("#corrs").append("button").html("Show Unexpected Correlations");
//    var expected_hide = d3.select("#corrs").append("button").html("Show All Correlations").style("visibility", "hidden");
//    var only_expected = d3.select("#corrs").append("button").html("Show Only Failed Correlations");
//
//    expected_show.on("click", function () {
//                             heatMap.transition().duration(1000)
//                                 .style("fill", function (d) {
//                                     if (d.expected)
//                                         return 'rgb(255,255,255)';
//                                     else return colorScale(d.corr);
//                                     });
//                             expected_show.style("visibility", "hidden");
//                             expected_hide.style("visibility", "visible");
//                         });
//
//    only_expected.on("click", function () {
//                             heatMap.transition().duration(1000)
//                                 .style("fill", function (d) {
//                                     if (d.expected && -0.5 < d.corr && d.corr < 0.5 )
//                                         return colorScale(d.corr);
//                                     else return 'rgb(255,255,255)';
//                                     });
//                             only_expected.style("visibility", "hidden");
//                             expected_hide.style("visibility", "visible");
//                         });
//
//    expected_hide.on("click", function () {
//                             heatMap.transition().duration(1000)
//                                 .style("fill", function (d) { return colorScale(d.corr); });
//                             expected_show.style("visibility", "visible");
//                             expected_hide.style("visibility", "hidden");
//                             only_expected.style("visibility", "visible");
//                        });


    };