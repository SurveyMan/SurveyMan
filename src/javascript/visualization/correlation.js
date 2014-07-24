var display_correlations = (function (globals) {

    return function() {

        var NaNOrZero = function (d) {
            if (isNaN(d.corr)) {
                return 0;
            } return d.corr;
        };

        var makeCorrChart = function (d) {

            var height = 400,
                width = height,
                top = 150,
                bottom = 25,
                left = 25,
                right = 150,
                tickLength = 5,
                radius = 5,
                padding = 2,
                xInterval = width / d.q1.options.length,
                yInterval = height / d.q2.options.length,
                numResponsesX = Math.floor(xInterval / (radius * 2 + padding)),
                numResponsesY = Math.floor(yInterval / (radius * 2 + padding)),
                process_data = function (d) {
                    return {
                        trueResponses : _.filter(d, trueResponse)
                    };
                },
                data = _.filter(getJsonizedResponses()
                        , function (r) {
                                return _.filter(r, function (qr) { return qr.q === d.q1.id; }).length !== 0
                                    && _.filter(r, function (qr) { return qr.q === d.q2.id; }).length !== 0
                            }
                        ),
                getResponseOpt = function (q) {
                    return function(resp) {
                        return _.indexOf(_.pluck(q.options, "id")
                                         , _.filter(resp, function (r) { return r.q === q.id; })[0].opts[0].o);
                    };
                },
                getInteriorOffset = function(_d, i) {
                    var allPrecedingData = _.map(_.first(data, i), function (a) { 
                                return _.map(a, function (b) { 
                                        return b.opts[0]; 
                                }); 
                        });
                    var precedingOpts = _.filter(allPrecedingData, function (b) {
                            return _.filter(b, function (c) { return c.o === d.q1.options[getResponseOpt(d.q1)(_d)].id; }).length === 1
                                && _.filter(b, function (c) { return c.o === d.q2.options[getResponseOpt(d.q2)(_d)].id; }).length === 1;
                        });
                    return precedingOpts.length;
                };

            var svg = d3.selectAll("#respComp").append("svg")
                .attr("width", width + left + right + 10)
                .attr("height", height + top + bottom)
                .append("g")
                .attr("transform", "translate(" + left + "," + top + ")");
                
            svg.append("rect")
                .attr("height", height)
                .attr("width", width)
                .attr("stroke", "black")
                .attr("stroke-width", 2)
                .attr("fill", "white");
                
            svg.append("text")
                .text(d.q1.qtext)
                .attr("y", height + 15);
                
            svg.append("text")
                .text(d.q2.qtext)
                .style("text-anchor", "start")
                .attr("x", -height)
                .attr("y", -5)
                .attr("transform", "rotate(270)");

            svg.selectAll(".xLab")
                    .data(d.q1.options).enter()
                    .append("text")
                    .text(function (d) { return d.otext; })
                    .attr("transform", function (d,i) { return "translate("+((i * xInterval) + (xInterval / 2)) + ", -"+padding+")  rotate(-30) "; })
                    .style("text-anchor", "start");

            svg.selectAll(".yLab")
                .data(d.q2.options).enter()
                .append("text")
                .text(function (d) { return d.otext; })
                .attr("x", width + padding)
                .attr("y", function (d,i) { return (i * yInterval) + (yInterval / 2); })
                .style("text-anchor", "start");
           
           svg.selectAll(".xline")
                .data(_.rest(_.range(d.q1.options.length))).enter()
                .append("line")
                .attr("x1", function (d) { return d * xInterval; })
                .attr("x2", function (d) { return d * xInterval; })
                .attr("y1", -tickLength)
                .attr("y2", height)
                .attr("stroke", "black")
                .attr("stroke-width", 1);
                
           svg.selectAll(".yline")
                .data(_.rest(_.range(d.q1.options.length))).enter()
                .append("line")
                .attr("x2", width + tickLength)
                .attr("y1", function (d) { return d * yInterval; })
                .attr("y2", function (d) { return d * yInterval; })
                .attr("stroke", "black")
                .attr("stroke-width", 1);
                
           svg.selectAll("circle")
                .data(data).enter()
                .append("circle")
                .attr("r", radius)
                .attr("cx", function (_d, i) {
                    var offset = getResponseOpt(d.q1)(_d) * xInterval;
                    var interiorOffset = (getInteriorOffset(_d, i) % numResponsesX) * radius * 2;
                    return offset
                        + interiorOffset
                        + padding
                        + radius;
                })
                .attr("cy", function (_d, i) {
                    var offset = getResponseOpt(d.q2)(_d) * yInterval;
                    return offset
                        + (Math.floor(getInteriorOffset(_d, i) / numResponsesY) * radius * 2)
                        + padding
                        + radius;
                })
                .attr("fill", "gray")
                .attr("cursor", "pointer")
                .on("click", function (d) { zoomResponse(process_data(d), "respComp"); })
                .append("title")
                .text(function (_d, i) {
                    return d.q1.options[getResponseOpt(d.q1)(_d)].otext 
                        + ", " 
                        + d.q2.options[getResponseOpt(d.q2)(_d)].otext;
                });




        };

        var getCorrelationData = function (d) {
            var div = $.parseHTML("<div class='col-md-12' id='respComp'>"
            +"<p>Click on the grey circles to see individual responses.</p>"
                +"<p><b>#/Responses</b>:&nbsp;"+d.ct1
                +"&nbsp;&nbsp;<b>Coeff</b>:&nbsp;"+d.coeff
                +"&nbsp;&nbsp;<b>Val</b>:&nbsp;"+d.corr
                +"</p>"
                +"</div>");
            $(div).css("margin-top" , "10px");
            return div;
        };

        var zoomCorrelation = function(d) {
            // hide heatmap
            $("#corrs").hide();
            $("#questionCloseup").empty();
            $("#questionCloseup").append(getCorrelationData(d));
            var qDiv = $.parseHTML("<div style='background:#FFFAFA'></div>">);
            $(qDiv).append(getQuestionHTML(d.q1));
            $(qDiv).append(getQuestionHTML(d.q2));
            $("#questionCloseup").append(qDiv);
            $("#questionCloseup").append("<div class=\"col-md-10 block-center\"><button class=\"btn\" onclick='$(\"#questionCloseup\").empty(); $(\"#corrs\").show();'>Return to Heatmap</button></div>");
            makeCorrChart(d);
            $("#questionCloseup").show();
        };

        var questions = _.map(_.filter(globals.sm.survey.questions
                                       , function(_q) { return !_q.freetext;})
                              , function (_q) { return _q.id; }),
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
                        return {
                            q1 : globals.sm.getQuestionById(_d.q1),
                            ct1 : +_d.ct1,
                            q2 : globals.sm.getQuestionById(_d.q2),
                            ct2 : +_d.ct2,
                            corr : parseFloat(_d.val),
                            coeff : _d.coeff,
                            expected : JSON.parse(_d.expected)
                        };
                    },
                colorScale = d3.scale.quantile()
                                  .domain([-1.0, 1.0])
                                  .range(colors);

        var svg = d3.select("#corrs").append("svg")
                                  .attr("width", width + margin.left + margin.right)
                                  .attr("height", height + margin.top + margin.bottom)
                                  .append("g")
                                  .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var heatMap = svg.selectAll("#corrs")
                        .data(_.map(globals.corrs, processCorrs))
                        .enter()
                        .append("rect")
                        .attr("x", function(d) { return _.indexOf(questions, d.q1.id) * gridSize; })
                        .attr("y", function(d) { return _.indexOf(questions, d.q2.id) * gridSize; })
                        .attr("rx", 4)
                        .attr("ry", 4)
                        .attr("class", "hour bordered")
                        .attr("width", gridSize)
                        .attr("height", gridSize)
                        .style("fill", function (d) { return colorScale(d.corr); });


        heatMap.attr("cursor", "pointer")
            .on("click", function (d, i) {
                    zoomCorrelation(d);
                    return;
                });

        heatMap.append("title").text(function(d) { return d.q1.qtext + "\n" + d.q2.qtext + "\nval: " + d.corr; });

        var legend = svg.selectAll(".legend")
            .data(threshholds)
            .enter().append("g")
            .attr("class", "legend");

        legend.append("rect")
          .attr("x", function(d) { return legendElementWidth * _.indexOf(threshholds, d); })
          .attr("y", globals.sm.survey.questions.length*gridSize)
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
})(globals);
